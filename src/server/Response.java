package server;

import java.io.*;

/**
 * HTTP 响应类
 * 读取静态资源数据，写入到响应流中
 */
public class Response {
    private static final String WEB_ROOT = System.getProperty("user.dir")
            + File.separator
            + "webroot";

    private static final int WRITE_BUFFER_SIZE = 1024;
    private OutputStream outputStream;

    private Request request;

    public Response(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void sendStaticResource() throws IOException {
        // 获取uri(即文件资源路径)
        String uri = request.getUri();
        File file = new File(WEB_ROOT + uri);
        FileInputStream fileInputStream = null;
        try {
            if (file.exists()) {
                fileInputStream = new FileInputStream(file);
                byte[] fileContentBuffer = new byte[WRITE_BUFFER_SIZE];
                int ch = fileInputStream.read(fileContentBuffer);
                while (ch != -1) {
                    outputStream.write(fileContentBuffer,0, ch);
                    ch = fileInputStream.read(fileContentBuffer);
                }
                // 网络I/O流需要通过flush保证网络包及时发送
                outputStream.flush();
            }else {
                String errorMessage = "HTTP/1.1 404 FIle Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 23\r\n" +
                        "\r\n" +
                        "<h1>File Not Found</h1>";

                outputStream.write(errorMessage.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }
}
