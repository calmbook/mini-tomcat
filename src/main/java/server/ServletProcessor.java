package server;

import org.apache.commons.lang3.text.StrSubstitutor;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
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
    private ServletContainer container;
    public ServletProcessor(ServletContainer container) {
        this.container = container;
    }

    public void process(HttpRequest request, HttpResponse response) throws IOException, ServletException {
        this.container.invoke(request,response);
    }

}
