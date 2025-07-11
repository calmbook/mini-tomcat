package server;

import java.io.*;

/**
 * HTTP 响应类
 * 响应实体类
 */
public class Response {
    private OutputStream outputStream;

    public Response(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
