package test;

import server.Request;
import server.Response;
import server.Servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class HelloServlet implements Servlet {
    @Override
    public void service(Request request, Response response) throws IOException {
        String uri = request.getUri();
        String retMsg = String.format("当前时间为: %s, 资源路径为：" , LocalDateTime.now(), request.getUri());
        response.getOutputStream().write(retMsg.getBytes(StandardCharsets.UTF_8));
    }
}
