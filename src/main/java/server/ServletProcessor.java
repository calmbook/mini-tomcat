package server;

import org.apache.commons.lang3.text.StrSubstitutor;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * 根据URI定位Servlet实例，调用统一的service方法，完成动态资源获取（业务逻辑的执行）
 */
public class ServletProcessor {
    private static String OKMessage = "HTTP/1.1 ${StatusCode} ${StatusName}\r\n" +
            "Content-Type: ${ContentType}\r\n" +
            "Content-Length: ${ContentLength}\r\n" +
            "Server: minit\r\n" +
            "Date: ${ZonedDateTime}\r\n" +
            "\r\n";

    public void process(HttpRequest request, HttpResponse response) throws IOException {
        // 获取URI，拼接完整的Java类名称
        String uri = request.getUri();
        // 指定目录加载
        URLClassLoader loader = createClassLoader();

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 业务执行成功响应
        // 这里如果业务执行失败了，成功响应也会发出去了，是有问题的
        // tomcat中的解决方案是设置一个状态位，然后包装了一下PrintWriter，不是每次println都flush，而是缓存满了才flush，flush之后修改状态位，不能再修改Header
        // 当然这里简单的把getWriter中的autoFlush关掉也可以实现延迟发送响应头的效果（但不能避免用户修改响应头之后重复发送），PrintWriter自带缓存，不会每次都flush的
        setSuccessResponseHead(response);
        response.sendHeaders();

        try {
            // 加载业务类，构建Servlet实例
            Class<?> servletClass = loadServletClassFromURI(loader, uri);
            // 调用Servlet实例方法，完成业务逻辑
            Servlet servlet = null;
            servlet = (Servlet) servletClass.newInstance();
            // 使用门面模式封装，避免业务应用强转request,response
            HttpRequestFacade requestFacade = new HttpRequestFacade(request);
            HttpResponseFacade responseFacade = new HttpResponseFacade(response);
            servlet.service(requestFacade,responseFacade);
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
        System.out.println("servlet 实例类名: " + servletName);
        Class<?> servletClass = null;
        try {
            servletClass = loader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("servlet类[%s]未找到", servletName));
        }
        return servletClass;
    }

    private void setSuccessResponseHead(HttpResponse response) {
        response.setStatus(SC_OK);
        response.setHeader("Content-Type", "text/plain;charset=utf-8");
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
