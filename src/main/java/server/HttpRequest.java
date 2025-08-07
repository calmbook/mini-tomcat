package server;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HttpRequest implements HttpServletRequest {
    private SocketInputStream socketInputStream;
    private String uri;
    InetAddress address;
    int port;
    protected HashMap<String, String> headers = new HashMap<>();
    protected Map<String, String[]> parameters = new ConcurrentHashMap<>();
    HttpRequestLine requestLine = new HttpRequestLine();
    /**
     * url上的参数字符串
     */
    private String queryString;

    private String sessionid;
    private boolean paramParsed = false;

    private Cookie[] cookies;
    private SessionFacade sessionFacade;
    private HttpResponse response;

    public HttpRequest(InputStream input) {
        this.socketInputStream = new SocketInputStream(input, 2048);
    }

    public void parse(Socket socket) {
        try {
            parseConnection(socket);
            // 解析请求首行，http method, uri, protocol
            this.socketInputStream.readRequestLine(requestLine);
            parseRequestLine();
            // 解析请求头
            parseHeaders();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    /**
     * uri中通过问号分隔请求路径以及参数信息
     * 多个参数信息之间通过&号分隔
     * jessionid通过分号分隔
     * exp:
     * http://example.com/app/path;jsessionid=ABCDEF1234567890?param1=value1&m2=value2
     */
    private void parseRequestLine() {
        int questionIndex = requestLine.indexOf("?");
        if (questionIndex != -1) {
            this.uri = new String(requestLine.uri, 0, questionIndex);
            this.queryString = new String(requestLine.uri, questionIndex + 1, requestLine.uriEnd);
        } else {
            this.uri = new String(requestLine.uri, 0, requestLine.uriEnd);
        }
        // 截取uri中的sessionid
        String tmp = ";" + DefaultHeaders.JSESSIONID_NAME + "=";
        int semicolon = uri.indexOf(tmp);
        if (semicolon >= 0) {
            sessionid = uri.substring(semicolon+DefaultHeaders.JSESSIONID_NAME.length());
            uri = uri.substring(0, semicolon);
        }
    }

    private void parseConnection(Socket socket) {
        address = socket.getInetAddress();
        port = socket.getPort();
    }

    private void parseHeaders() throws IOException, ServletException {
        while (true) {
            HttpHeader header = new HttpHeader();
            socketInputStream.readHeader(header);
            // name和value都没读到说明已经读完了，退出循环
            if (header.nameEnd == 0) {
                if (header.valueEnd == 0) {
                    return;
                } else {
                    throw new ServletException("httpProcessor.parseHeaders.colon");
                }
            }
            String name = new String(header.name, 0, header.nameEnd).toLowerCase();
            String value = new String(header.value, 0, header.valueEnd);
            headers.put(name, value);
            if (name.equals(DefaultHeaders.COOKIE_NAME)) {
                Cookie[] cookiesArr = parseCookieHeader(value);
                this.cookies = cookiesArr;
                for (Cookie cookie : this.cookies) {
                    if (cookie.getName().equals("jsessionid")) {
                        this.sessionid = cookie.getValue();
                    }
                }
            } else if (name.equals(DefaultHeaders.CONNECTION_NAME)) {
                if (value.equals("close")) {
                    response.setHeader("Connection", "close");
                }
            } else if (name.equals(DefaultHeaders.TRANSFER_ENCODING_NAME)) {
                response.setHeader("Transfer-Encoding", value);
            }


            // 目前看下面这段代码没什么意义
            // Set the corresponding request headers
//            if (name.equals(DefaultHeaders.ACCEPT_LANGUAGE_NAME)) {
//                headers.put(name, value);
//            } else if (name.equals(DefaultHeaders.CONTENT_LENGTH_NAME)) {
//                headers.put(name, value);
//            } else if (name.equals(DefaultHeaders.CONTENT_TYPE_NAME)) {
//                headers.put(name, value);
//            } else if (name.equals(DefaultHeaders.HOST_NAME)) {
//                headers.put(name, value);
//            } else if (name.equals(DefaultHeaders.CONNECTION_NAME)) {
//                headers.put(name, value);
//            } else if (name.equals(DefaultHeaders.TRANSFER_ENCODING_NAME)) {
//                headers.put(name, value);
//            } else {
//                headers.put(name, value);
//            }
        }
    }

    /**
     * exp
     * GET /sample_page.html HTTP/1.1
     * Host: www.example.org
     * Cookie: yummy_cookie=choco; tasty_cookie=strawberry
     * @param header header头中的cookie字符串
     * @return
     */
    public  Cookie[] parseCookieHeader(String header) {
        if ((header == null) || (header.length() < 1) )
            return (new Cookie[0]);
        ArrayList<Cookie> cookieal = new ArrayList<>();
        while (header.length() > 0) {
            //分隔多个cookie字符串
            int semicolon = header.indexOf(';');
            // 没找到，说明只有一个Cookie
            if (semicolon < 0)
                semicolon = header.length();
            // header只剩一个分号了，通常是最后一个字符加了一个没必要的分号
            if (semicolon == 0)
                break;

            // 取第一个cookie
            String cookie = header.substring(0, semicolon);
            // 将header中已经解析过的部分截取掉
            if (semicolon < header.length())
                header = header.substring(semicolon + 1);
            else
            // 分号位置索引和header长度相等，header直接设置位空字符串，下次循环退出去
                header = "";

            // 解析cookie中的name和value
            try {
                int equals = cookie.indexOf('=');
                if (equals > 0) {
                    String name = cookie.substring(0, equals).trim();
                    String value = cookie.substring(equals+1).trim();
                    cookieal.add(new Cookie(name, value));
                }
            } catch (Throwable e) {
            }
        }
        return ((Cookie[]) cookieal.toArray (new Cookie [cookieal.size()]));
    }

    protected void parseParameters() {
        String encoding = getCharacterEncoding();
        System.out.println(encoding);
        // 解析请求行中的请求参数
        parseUrlQueryString(encoding);

        // 解析请求体中的参数
        parseBodyParam(encoding);
    }

    private void parseBodyParam(String encoding) {
        String contentType = getContentType();
        if (contentType == null)
            contentType = "";
        // 分号主要用来区分请求类型和编码方式，例如：application/json; charset=utf-8
        int semicolon = contentType.indexOf(';');
        if (semicolon >= 0) {
            contentType = contentType.substring(0, semicolon).trim();
        }
        else {
            contentType = contentType.trim();
        }
        // 目前只解析x-www-form-urlencoded这种格式
        if ("POST".equals(getMethod()) && (getContentLength() > 0)
                && "application/x-www-form-urlencoded".equals(contentType)) {
            try {
                byte[] buf = readAndValidRequestContent();
                parseParameters(this.parameters, buf, encoding);
            }
            catch (UnsupportedEncodingException ue) {
            }
            catch (IOException e) {
                throw new RuntimeException("Content read fail");
            }
        }
    }

    private byte[] readAndValidRequestContent() throws IOException {
        int max = getContentLength();
        int len = 0;
        byte buf[] = new byte[getContentLength()];
        ServletInputStream is = getInputStream();
        while (len < max) {
            int next = is.read(buf, len, max - len);
            if (next < 0) {
                break;
            }
            len += next;
        }
        is.close();
        if (len < max) {
            throw new RuntimeException("Content length mismatch");
        }
        return buf;
    }

    private void parseUrlQueryString(String encoding) {
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }
        String qString = getQueryString();
        System.out.println("getQueryString:"+qString);
        if (qString != null) {
            byte[] bytes = new byte[qString.length()];
            try {
                bytes=qString.getBytes(encoding);
                parseParameters(this.parameters, bytes, encoding);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();;
            }
        }
    }

    /**
     * 将字符串转化为数字，例如字符串‘2’变成十进制数字2，字符串'f'变成十进制数字15
     * 此处主要是用于解码由浏览器编码的特殊字符，例如空格（%20）
     *
     * @param b
     * @return
     */
    private byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) return (byte)(b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte)(b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte)(b - 'A' + 10);
        return 0;
    }

    /**
     *
     * @param map 参数集合
     * @param data 参数字节数组
     * @param encoding 编码方式
     * @throws UnsupportedEncodingException
     */
    public void parseParameters(Map<String,String[]> map, byte[] data, String encoding)
            throws UnsupportedEncodingException {
        if (paramParsed)
            return;
        System.out.println(data);
        if (data != null && data.length > 0) {
            int    pos = 0;
            // input index
            int    ix = 0;
            // output index
            int    ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                    case '&':
                        value = new String(data, 0, ox, encoding);
                        if (key != null) {
                            putMapEntry(map,key, value);
                            key = null;
                        }
                        ox = 0;
                        break;
                    case '=':
                        key = new String(data, 0, ox, encoding);
                        ox = 0;
                        break;
                    // 早期HTTP规范中+号用来替代空格，现在通常用%20编码来表示
                    case '+':
                        data[ox++] = (byte)' ';
                        break;
                    // 将十六进制转化为十进制，例如0x20 -》 32，转换方式是 2*16 + 0
                    // 乘以16可以通过左移4位实现，效率更高
                    case '%':
                        data[ox++] = (byte)((convertHexDigit(data[ix++]) << 4)
                                + convertHexDigit(data[ix++]));
                        break;
                    default:
                        data[ox++] = c;
                }
            }
            //The last value does not end in '&'.  So save it now.
            if (key != null) {
                value = new String(data, 0, ox, encoding);
                putMapEntry(map,key, value);
            }
        }
        paramParsed = true;
    }

    /**
     * 主要用于处理value是数组的情况
     * @param map 原 map
     * @param name key值
     * @param value value值
     */
    private static void putMapEntry( Map<String,String[]> map, String name, String value) {
        String[] newValues = null;
        String[] oldValues = (String[]) map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

    public String getUri() {
        return this.uri;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public Object getAttribute(String arg0) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return headers.get(DefaultHeaders.TRANSFER_ENCODING_NAME);
    }

    @Override
    public int getContentLength() {
        return Integer.parseInt(headers.get(DefaultHeaders.CONTENT_LENGTH_NAME));
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return headers.get(DefaultHeaders.CONTENT_TYPE_NAME);
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
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
    public String getParameter(String name) {
        parseParameters();
        String[] values = parameters.get(name);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String arg0) {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRealPath(String arg0) {
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
    public int getRemotePort() {
        return 0;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
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
    public ServletContext getServletContext() {
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
    public boolean isSecure() {
        return false;
    }

    @Override
    public void removeAttribute(String arg0) {

    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
    }

    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        return false;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return this.cookies;
    }

    @Override
    public long getDateHeader(String arg0) {
        return 0;
    }

    @Override
    public String getHeader(String arg0) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String arg0) {
        return null;
    }

    @Override
    public int getIntHeader(String arg0) {
        return 0;
    }

    @Override
    public String getMethod() {
        return new String(requestLine.method,0, requestLine.methodEnd);
    }

    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return this.sessionFacade;
    }

    //如果有存在的session，直接返回，如果没有，创建一个新的session
    public HttpSession getSession(boolean create) {
        if (sessionFacade != null)
            return sessionFacade;
        HttpSession session = null;
        if (sessionid != null) {
            session = HttpConnector.sessions.get(sessionid);
            if (session != null) {
                sessionFacade = new SessionFacade(session);
                return sessionFacade;
            } else {
                session = HttpConnector.createSession();
                sessionFacade = new SessionFacade(session);
                return sessionFacade;
            }
        } else {
            session = HttpConnector.createSession();
            sessionFacade = new SessionFacade(session);
            sessionid = session.getId();
            return sessionFacade;
        }
    }
    public String getSessionId() {
        return this.sessionid;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return false;
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        return null;
    }
}
