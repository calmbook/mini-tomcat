package server;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP 请求对象
 */
public class Request {
    private InputStream inputStream;
    private String uri;


    public Request(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * 从input流中读取HTTP报文，解析出url（在本示例中即为静态资源地址）
     *
     */
    public void parse() {
        // 固定读取2KB数据，此处一个线程一个HTTP请求，不考虑多个请求用一个inputStream的问题
        byte[] buffer = new byte[2048];

        int i;
        try {
            // read方法返回的是读到buffer中的总字节数（这里不考虑大于2KB的情况）
            System.out.println("阻塞读取HTTP请求报文");
            i = inputStream.read(buffer);
        } catch (IOException e) {
            i = -1;
            e.printStackTrace();
        }

        // 未读取到请求内容，不进行解析
        if (i == -1) {
            return;
        }

        // 将读取到buffer缓存中的数据抽取出来
        byte[] destArr = new byte[i];
        System.arraycopy(buffer, 0, destArr, 0, i);
        String httpRequestPkg = new String(destArr);
        System.out.println("http请求报文为" + httpRequestPkg);

        this.uri = getUriFromHttpRequest(httpRequestPkg);
    }

    /**
     * 解析HTTP协议第一行数据，得到URI
     * exp: 需要从 “GET /hello.txt HTTP/1.1” 截取出 /hello.txt
     * @param httpRequestPkg http完整请求报文
     */
    private String getUriFromHttpRequest(String httpRequestPkg) {
        int firstSpaceIndex = httpRequestPkg.indexOf(" ");
        if (firstSpaceIndex == -1) {
            return null;
        }
        int secendSpaceIndex = httpRequestPkg.indexOf(" ", firstSpaceIndex + 1);
        if (secendSpaceIndex == -1) {
            return null;
        }
        // subString 左闭右开，所以开始位置为第一个空格处位置+1，结束位置是第二个索引
        return httpRequestPkg.substring(firstSpaceIndex + 1, secendSpaceIndex);
    }

    public String getUri() {
        return uri;
    }
}
