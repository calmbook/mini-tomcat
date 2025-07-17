package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpConnector implements Runnable{
    private HttpProcessorPool processorPool = new HttpProcessorPool(10, 20);

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        processorPool.init();
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
            Socket socket = null;
            try {
                // 阻塞方法，新的请求进来之前会阻塞在这里
                System.out.println("服务端等待连接建立中......");
                socket = serverSocket.accept();
                System.out.println("建立连接成功," + "客户端地址为： " + socket.getInetAddress() + ":" + socket.getPort());

                // processor异步化处理
                // 不直接调用process处理业务逻辑，仅仅把socket传进去，由processor线程异步处理
                HttpProcessor httpProcessor = processorPool.getProcessor();
                httpProcessor.assign(socket);

                System.out.println("请求处理完成");
                // processor 异步处理后，socket需要由业务线程自己关闭
//                socket.close();
            } catch (IOException e) {
                e.printStackTrace();

                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
