package test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class TestServlet1 extends HttpServlet{
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
        System.out.println("Enter doGet()");
        System.out.println("parameter name : "+request.getParameter("name"));

        HttpSession session = request.getSession(true);

        Integer count = (Integer) session.getAttribute("count");
        if (count == null) {
            count = 1;
            session.setAttribute("count", 1);
        } else {
            count++;
            session.setAttribute("count", count);
        }
        System.out.println("get count from session : " + count);

        System.out.println("::::::::call count ::::::::: " + count);
        if (count > 2) {
            response.addHeader("Connection", "close");
        }
//        String retMsg = String.format("当前线程名为: %s, 当前时间为: %s", Thread.currentThread().getName(), LocalDateTime.now());
//        response.getWriter().println(retMsg);

        response.setCharacterEncoding("UTF-8");

        String doc = "Hello";

        StringBuilder builder = new StringBuilder();
        builder.append(Integer.toHexString(doc.getBytes(StandardCharsets.UTF_8).length))
                .append("\r\n")
                .append(doc)
                .append("\r\n")
                .append("0\r\n\r\n");
        System.out.println("分块传输内容: " + builder);
        PrintWriter writer = response.getWriter();
        writer.print(builder);
        writer.flush();
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
        System.out.println("Enter doGet()");
        System.out.println("parameter name : "+request.getParameter("name"));
        response.setCharacterEncoding("UTF-8");
        String doc = "<!DOCTYPE html> \n" +
                "<html>\n" +
                "<head><meta charset=\"utf-8\"><title>Test</title></head>\n"+
                "<body bgcolor=\"#f0f0f0\">\n" +
                "<h1 align=\"center\">" + "Test 你好" + "</h1>\n";
        System.out.println(doc);
        response.getWriter().println(doc);

    }
}
