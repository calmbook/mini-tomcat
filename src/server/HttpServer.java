package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    public static void main(String[] args) {
        new HttpServer().start();
    }

    private void start() {
        // 循环接收网络请求，建立连接（目前只考虑一个端口）
        ServerSocket serverSocket = null;
        try {
            // backlog为1，即网络连接的请求队列数为1，超过一个就会拒绝，所以该服务器不支持并发，只支持串行处理请求资源
            serverSocket = new ServerSocket(8080, 1, InetAddress.getByName("127.0.0.1"));
            System.out.println(String.format("服务启动成功，服务器地址为：%s, 本地监听端口为，%s",
                    serverSocket.getLocalSocketAddress(),
                    serverSocket.getLocalPort()));
        } catch (IOException e) {
            e.printStackTrace();
            // 直接停止程序，不再交给上层处理异常的机会
            System.exit(1);
        }
        // 一直接收新的请求
        while (true) {
            try {
                // 阻塞方法，新的请求进来之前会阻塞在这里
                System.out.println("服务端等待连接建立中......");
                Socket socket = serverSocket.accept();
                System.out.println("建立连接成功," + "客户端地址为： " + socket.getInetAddress() + ":" + socket.getPort());
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                // 读取数据，基于HTTP协议解析请求,得到URI，输入参数等关键值（这里只解析URI）
                // request从网络读取数据时也是阻塞的，没有数据会一直等
                Request request = new Request(inputStream);
                request.parse();
                System.out.println("HTTP请求解析完成，URI为" + request.getUri());

                // 将响应结果写回网络输出
                Response response = new Response(outputStream);
                response.setRequest(request);
                response.sendStaticResource();
                System.out.println("将文本内容写回响应成功");

                // 关闭连接
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
