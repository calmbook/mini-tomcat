package server;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 解析请求首行，请求头，请求参数
 *
 * exp:
 *
 * GET /api/data HTTP/1.1
 * Host: example.com
 * User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
 * Accept: application/json, text/plain
 * Accept-Language: en-US,en;q=0.9
 * Connection: keep-alive
 */
public class SocketInputStream extends ServletInputStream {
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte SP = (byte) ' ';
    private static final byte HT = (byte) '\t';
    private static final byte COLON = (byte) ':';
    private static final int LC_OFFSET = 'A' - 'a';

    protected byte buf[];
    /**
     * 已经读取的字节数
     */
    protected int count;
    /**
     * 当前字节索引位置（）
     */
    protected int pos;

    protected InputStream is;

    public SocketInputStream(InputStream is, int bufferSize) {
        this.is = is;
        buf = new byte[bufferSize];
    }

    /**
     * 读取请求行
     * @param requestLine 请求行对象
     * @throws IOException
     */
    public void readRequestLine(HttpRequestLine requestLine)
        throws IOException {

        // 找到第一个非换行字符的位置，并设置pos的值
        findFirstNonCROrLFIndex();

        parseHttpMethod(requestLine);

        parseURI(requestLine);

        parseProtocol(requestLine);
    }

    private void parseProtocol(HttpRequestLine requestLine) throws IOException {
        int readCount = 0;
        boolean eol = false;
        while (!eol) {
            readToBufferWhenNoData();

            if (buf[pos] == CR) {
                // Skip CR.
            } else if (buf[pos] == LF) {
                eol = true;
            } else {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }

        requestLine.protocolEnd = readCount;
    }

    private void parseURI(HttpRequestLine requestLine) throws IOException {
        int readCount = 0;
        boolean space = false;
        while (!space) {
            readToBufferWhenNoData();
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.uriEnd = readCount - 1;
    }

    private void parseHttpMethod(HttpRequestLine requestLine) throws IOException {
        int readCount = 0;
        boolean space = false;
        while (!space) {
            readToBufferWhenNoData();
            // 如果当前缓冲区的字符是空格，说明本小节（HTTP方法）已经读完了，设置标志位退出
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];
            readCount++;
            // 预读数据指针后移1位
            pos++;
        }
        requestLine.methodEnd = readCount - 1;
    }

    private void findFirstNonCROrLFIndex() {
        int chr = 0;
        do {
            try {
                chr = read();
            } catch (IOException e) {
            }
        } while ((chr == CR) || (chr == LF));
        // 当前字节是换行符，所以将索引位置回退一个字节
        pos--;
    }

    public void readHeader(HttpHeader header)
        throws IOException {

        /**
         * HTTP 头以 \r\n（CRLF）分隔，但实际数据可能只有 \r 或 \n（不规范情况）。
         * 如果是 CR（\r，ASCII 13），则继续读取下一个字符（预期是 LF，\n，ASCII 10）
         * 如果是 LF（\n），直接结束当前行。
         * 如果是其他字符（非换行符），则回退指针 pos--，表示当前字符属于头字段内容，需要重新读取。
         */
        int chr = read();
        if ((chr == CR) || (chr == LF)) { // Skipping CR
            if (chr == CR) {
                read(); // Skipping LF
            }
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        } else {
            pos--;
        }

        // Reading the header name
        parseHeaderNameOfLine(header);

        // Reading the header value (which can be spanned over multiple lines)
        parseHeadValue(header);
    }

    private void parseHeadValue(HttpHeader header) throws IOException {
        int readCount = 0;
        boolean eol = false;
        boolean validLine = true;

        while (validLine) {
            boolean space = true;

            // Skipping spaces
            // Note : Only leading white spaces are removed. Trailing white
            // spaces are not.
            // 跳过value与冒号之间的空格或制表符
            while (space) {
                readToBufferWhenNoData();
                if ((buf[pos] == SP) || (buf[pos] == HT)) {
                    pos++;
                } else {
                    space = false;
                }
            }


            while (!eol) {
                readToBufferWhenNoData();
                if (buf[pos] == CR) {
                } else if (buf[pos] == LF) {
                    eol = true;
                } else {
                    // FIXME : Check if binary conversion is working fine
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount++;
                }
                pos++;
            }

            // 换行后的下一个字节
            int nextChr = read();

            /**
             * HTTP协议允许value跨行，但续行必须以SP或HT开头
             * exp:
             * 下面这个示例的意思是将换行和续行中的空格合并位空格（续行以SP或HT开头说明它不是一个新的header，而是当前header的另一个值）
             * Header: value
             *   continuation
             * 应解析为 "value continuation"
             */
            if (nextChr == SP || nextChr == HT) {
                eol = false;
                header.value[readCount] = ' ';
                readCount++;
            } else {
                pos--;
                validLine = false;
            }
        }

        header.valueEnd = readCount;
    }

    /**
     * 当前缓存数据读完之后重新read一批数据到缓存中
     * @throws IOException
     */
    private void readToBufferWhenNoData() throws IOException {
        // We're at the end of the internal buffer
        if (pos >= count) {
            // Copying part (or all) of the internal buffer to the line
            // buffer
            int val = read();
            if (val == -1)
                throw new IOException("requestStream.readline.error");
            pos = 0;
        }
    }

    private void parseHeaderNameOfLine(HttpHeader header) throws IOException {
        // Reading the header name
        int readCount = 0;
        boolean colon = false;

        while (!colon) {
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
            }
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
            // HTTP 头是忽略大小写的
            val = toLowerCase(val);
            header.name[readCount] = val;
            readCount++;
            pos++;
        }

        header.nameEnd = readCount - 1;
    }

    private static char toLowerCase(char val) {
        if ((val >= 'A') && (val <= 'Z')) {
            val = (char) (val - LC_OFFSET);
        }
        return val;
    }

    /**
     * 缓存一批数据，并返回下一个字节
     * @return 字节流中的下一个字节
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        // byte转int时，通过和0xff按位与，实现int的前24位补0，只保留低8位（即原来byte的那8位），将byte转换为无符号数
        // 即0xff在网络传输中指的是255，但是在字节转int的过程中会被转换成-1
        return buf[pos++] & 0xff;
    }

    public int available() throws IOException {
        return (count - pos) + is.available();
    }

    public void close() throws IOException {
        if (is == null) {
            return;
        }
        is.close();
        is = null;
        buf = null;
    }

    /**
     * 先预读到缓存中去
     * @throws IOException
     */
    protected void fill() throws IOException {
        pos = 0;
        count = 0;
        int nRead = is.read(buf, 0, buf.length);
        if (nRead > 0) {
            count = nRead;
        }
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }
}
