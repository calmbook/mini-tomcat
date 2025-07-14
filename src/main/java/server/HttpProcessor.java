package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor {

    public void process(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            // 读取数据，基于HTTP协议解析请求,得到URI，输入参数等关键值（这里只解析URI）
        // request从网络读取数据时也是阻塞的，没有数据会一直等
        Request request = new Request(inputStream);
        request.parse();
        if (request.getUri() == null) {
            System.out.println("请求资源路径解析异常");
            return;
        }
        System.out.println("HTTP请求解析完成，URI为" + request.getUri());

        // 构建响应对象
        Response response = new Response(outputStream);

        // 分不同资源类型处理请求逻辑
        String uri = request.getUri();

            if (uri.startsWith("/servlet")) {
                ServletProcessor servletProcessor = new ServletProcessor();
                servletProcessor.process(request,response);
            }else {
                StaticResourceProcessor staticResourceProcessor = new StaticResourceProcessor();
                staticResourceProcessor.process(request, response);
            }
        } catch (IOException e) {
            // 后续可以调整成返回500错误响应
            e.printStackTrace();
        }
    }
}
