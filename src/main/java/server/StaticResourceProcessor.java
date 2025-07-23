package server;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 静态资源处理器，根据URI定位静态资源，读取内容并写入HTTP响应流中
 *
 */
public class StaticResourceProcessor {
    private static final int WRITE_BUFFER_SIZE = 1024;

    private static String fileNotFoundMessage = "HTTP/1.1 404 File Not Found\r\n" +
            "Content-Type: text/html\r\n" +
            "Content-Length: 23\r\n" +
            "\r\n" +
            "<h1>File Not Found</h1>";

    private static String OKMessage = "HTTP/1.1 ${StatusCode} ${StatusName}\r\n" +
            "Content-Type: ${ContentType}\r\n" +
            "Content-Length: ${ContentLength}\r\n" +
            "Server: minit\r\n" +
            "Date: ${ZonedDateTime}\r\n" +
            "\r\n";


    public void process(HttpRequest request, HttpResponse    response) throws IOException {
        // 获取uri(即文件资源路径)
        String uri = request.getUri();
        OutputStream outputStream = response.getOutput();

        File file = new File(ServerConstant.WEB_ROOT + uri);
        FileInputStream fileInputStream = null;
        try {
            if (file.exists()) {
                //先写入响应头
                String responseHead = composeResponseHead(file);
                outputStream.write(responseHead.getBytes());
                fileInputStream = new FileInputStream(file);
                byte[] fileContentBuffer = new byte[WRITE_BUFFER_SIZE];
                int ch = fileInputStream.read(fileContentBuffer);
                while (ch != -1) {
                    outputStream.write(fileContentBuffer, 0, ch);
                    ch = fileInputStream.read(fileContentBuffer);
                }
                // 网络I/O流需要通过flush保证网络包及时发送
                outputStream.flush();
            } else {;
                outputStream.write(fileNotFoundMessage.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }

    private String composeResponseHead(File file) {
        long fileLength = file.length();
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("StatusCode", "200");
        valuesMap.put("StatusName", "OK");
        valuesMap.put("ContentType", "text/html;charset=utf-8");
        // 响应内容长度可以解决客户端接收时的粘包，拆包问题
        valuesMap.put("ContentLength", fileLength);
        valuesMap.put("ZonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now()));
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return sub.replace(OKMessage);
    }
}
