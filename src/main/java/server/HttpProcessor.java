package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class HttpProcessor implements Runnable {
    private Socket socket;

    // 是否接收到了新的连接
    private boolean hasNewSocket = false;

    private final HttpProcessorPool processorPool;

    private boolean keepAlive = false;

    public HttpProcessor(HttpProcessorPool processorPool) {
        this.processorPool = processorPool;
    }

    @Override
    public void run() {
        while (true) {
            // 阻塞等待直至有可用的连接进来
            Socket socket = awaitSocket();

            if (socket == null) {
                continue;
            }
            // 处理业务逻辑
            process(socket);

            // 关闭socket连接，归还资源池
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            processorPool.recycle(this);
        }
    }

    /**
     * 如果hasAcceptSocket为false, 说明没有socket进来,阻塞等待connector唤醒
     * 为了避免wihle循环判断一直消耗CPU，使用wait-notify机制，每次线程被唤醒后都会进行一次判断，避免虚假唤醒
     * @return connector 传入进来的新socket连接
     */
    public synchronized Socket awaitSocket() {
        while (!hasNewSocket) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 复制一个引用，如果processor处理业务逻辑时接收到了新连接，已经返回的引用不会被修改，正在处理的业务逻辑不会受到影响
        // 如果直接在业务逻辑中使用this.socket，那么业务逻辑执行过程中如果有新的连接进来，那么this.socket就被会修改掉，影响业务逻辑的正常执行
        Socket newSocket = this.socket;
        this.socket = null;

        // 将状态设置回去，表示当前连接已经接收完毕，可以接收新的连接
        // 这里没有在业务逻辑执行完成之后再设置hasNewSocket状态并唤醒connector线程，是为了业务逻辑处理过程中提前接收新的socket连接，减少等待-唤醒的时间
        this.hasNewSocket = false;
        // 通知所有connector线程，连接已经接收完成，可以接收新的连接了
        notifyAll();
        return newSocket;

    }

    /**
     * connector 等待当前processor线程处理完现有的连接（不是业务处理完，而是接收完即可）才能继续分配socket连接，否则connector只能阻塞等待
     * @return
     */
    public synchronized void assign(Socket socket) {
        while (hasNewSocket) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.socket = socket;
        this.hasNewSocket = true;

        // 通知processor线程有socket连接进来了
        notifyAll();
    }

    public void start() {
        new Thread(this).start();
    }

    public void process(Socket socket) {
        InputStream inputStream = null;
        OutputStream outputStream =null;
        // 读取数据，基于HTTP协议解析请求,得到URI，输入参数等关键值（这里只解析URI）
        // request从网络读取数据时也是阻塞的，没有数据会一直等
        HttpRequest request =null;
        // 构建响应对象
        HttpResponse response =null;
        try {
            inputStream = socket.getInputStream();
             outputStream = socket.getOutputStream();
            keepAlive = true;

            while(keepAlive) {
                // 读取数据，基于HTTP协议解析请求,得到URI，输入参数等关键值（这里只解析URI）
                // request从网络读取数据时也是阻塞的，没有数据会一直等
                request = new HttpRequest(inputStream);
                // 构建响应对象
                response = new HttpResponse(outputStream);
                request.setResponse(response);
                request.parse(socket);

                // handle session
                if (request.getSessionId() == null || request.getSessionId().equals("")) {
                    request.getSession(true);
                }

                if (request.getUri() == null) {
                    System.out.println("请求资源路径解析异常");
                    break;
                }
                System.out.println("HTTP请求解析完成，URI为" + request.getUri());
                response.setRequest(request);

                // 分不同资源类型处理请求逻辑
                String uri = request.getUri();

                if (uri.startsWith("/servlet")) {
                    ServletProcessor servletProcessor = new ServletProcessor();
                    servletProcessor.process(request,response);
                }else {
                    StaticResourceProcessor staticResourceProcessor = new StaticResourceProcessor();
                    staticResourceProcessor.process(request, response);
                }

                // 此处先简单设置成utf-8，实际应该读取请求头
                response.setCharacterEncoding("UTF-8");
                // 因为socket不一定会被关闭，所以强制flush一次，保证本次响应发送完成
                response.getWriter().flush();

                if ("close".equals(response.getHeader("Connection"))) {
                    keepAlive = false;
                }
            }

        } catch (IOException e) {
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
            try {
                response.sendHeaders();
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }
    }
}
