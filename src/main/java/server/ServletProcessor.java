package server;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 根据URI定位Servlet实例，调用统一的service方法，完成动态资源获取（业务逻辑的执行）
 */
public class ServletProcessor {
    private static final int WRITE_BUFFER_SIZE = 1024;
    private static final String WEB_ROOT = System.getProperty("user.dir")
            + File.separator
            + "webroot";

    private static String OKMessage = "HTTP/1.1 ${StatusCode} ${StatusName}\r\n" +
            "Content-Type: ${ContentType}\r\n" +
            "Content-Length: ${ContentLength}\r\n" +
            "Server: minit\r\n" +
            "Date: ${ZonedDateTime}\r\n" +
            "\r\n";

    public void process(Request request, Response response) throws IOException {
        // 获取URI，拼接完整的Java类名称
        String uri = request.getUri();
        OutputStream outputStream = response.getOutputStream();
        // 指定目录加载
        URLClassLoader loader = createClassLoader();

        // 加载业务类，构建Servlet实例
        Class<?> servletClass = loadServletClassFromURI(loader, uri);

        // 写响应头(这里未考虑业务执行失败的情况，都返回200)
        String responseHead = composeResponseHead();
        outputStream.write(responseHead.getBytes(StandardCharsets.UTF_8));

        // 调用Servlet实例方法，完成业务逻辑
        Servlet servlet = null;
        try {
            servlet = (Servlet) servletClass.newInstance();
            servlet.service(request,response);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static URLClassLoader createClassLoader() {
        URLClassLoader loader = null;
        try {
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(ServerConstant.WEB_ROOT);
            // classPath.getCanonicalPath() 获取文件的规范路径
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString();
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loader;
    }

    /**
     *
     * @param loader 类加载器
     * @param uri 资源路径，exp: /test.HelloServlet
     */
    private Class<?>  loadServletClassFromURI(URLClassLoader loader, String uri) {
        // 从URI中获取Servlet实例类的路径(最后一个 '/' 后面的路径即为Servlet名称)
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        Class<?> servletClass = null;
        try {
            servletClass = loader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return servletClass;
    }

    private String composeResponseHead() {
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("StatusCode", "200");
        valuesMap.put("StatusName", "OK");
        valuesMap.put("ContentType", "text/html;charset=utf-8");
        valuesMap.put("ZonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now()));
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return sub.replace(OKMessage);
    }
}
