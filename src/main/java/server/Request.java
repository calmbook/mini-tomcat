package server;

import javax.servlet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP 请求对象
 */
public class Request implements ServletRequest {
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

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getParameter(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String s) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}
