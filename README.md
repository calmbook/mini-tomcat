# mini-tomcat
从零开始写一个简易版本的tomcat

# 参考资料
https://gitee.com/wang-kh/minit-learning-demo/

# 1.0
## 功能需求
实现一个静态资源服务器

用户在浏览器输入下面这个地址
```
http://localhost:8080/hello.txt`
```
服务端返回文本文件内容，浏览器展示文本内容

### 浏览器请求报文
```
GET /hello.txt HTTP/1.1
Host: localhost:8080
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2
Accept-Encoding: gzip, deflate, br, zstd
Connection: keep-alive
Upgrade-Insecure-Requests: 1
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: none
Sec-Fetch-User: ?1
Priority: u=0, i
```

## 实现流程
1. 后端启动一个服务器，监听8080端口，循环接收新的连接
2. 读取TCP连接的请求数据报文，根据HTTP请求报文规范解析出URI
3. 根据URI(文件名)获取文件资源
4. 读取文件内容并写入到HTTP响应中

## 相关知识点
1. Java I/O 编程（阻塞I/O读取数据）
2. Java 网络编程（基本步骤:监听-》建连接-》读数据-》写数据）
3. URL和URI的区别
    ```
    URI像身份证号,唯一标识一个人,不包含如何找到这个人的信息

    URL像家庭住址,不仅标识这个人,还告诉你如何到达这个位置
    ```
4. HTTP 协议内容（协议头第一行：GET /hello.txt HTTP/1.1）


# 2.0
## 功能需求
解决1.0版本中出现的一些问题
1. Response 没有按照HTTP协议响应头标准返回（经测试Chrome浏览器不识别这种响应结果）
2. 目前只支持读取静态资源，不支持让开发人员通过编码的方式自定义返回结果

最终达到的效果
1. 用户在浏览器输入下面这个地址 
   ```
      http://localhost:8080/hello.txt`
   ```
   服务端返回文本文件内容，浏览器展示文本内容
2. 用户在浏览器输入下面这个地址
   ```
      http://localhost:8080/servlet/test.HelloServlet
   ```
   服务端返回当前时间

### HTTP响应格式
```
HTTP/1.1 200 OK
Content-Type: text/html
Content-Length: 12

Hello World!
```
1. 第一行是状态行，表示使用 HTTP 协议、版本（1.1）、返回状态码
（200）以及返回状态名称（OK），中间由一个空格分隔
2. Content-Type: text/html 和 Content-Length: 12 则是以键值对的形式展示的返回头（Header），依行排列，这里面包含
对服务器和返回数据的描述。常用键的取值还有 Cookie、Authorization 等
3. 之后空一行，随后写入返回的内容（Hello World!），这些是服务器返回给客户端的具体数
据，包括但不限于文本、文件、图片等。我们把它叫做响应体

## 实现流程
1. 在Response中发送具体的内容之前加上协议头
2. 通过/servlet识别动态资源请求并获取动态实现类，通过反射调用对应的方法（通过Servlet接口固定方法定义）

## 改进思路
1. 将静态资源发送类和动态资源发送类分成2个类（单一职责，高内聚，低耦合）
2. 通过特定的路径标识Servlet请求，走不同的处理类逻辑

## 相关知识点
1. HTTP响应头规范
2. JVM 类加载方式


# 3.0
## 功能需求
1. 引入servlet-api包中的Servlet接口并实现它
2. 拆分连接器和处理器

## 实现流程与相关知识点
1. 通过Writer代替OutputStream写入（JAVA I/O编程，字符输出,PrintWriter，OutputStreamWriter,BufferedWriter）
2. 分离Connector和Processor（单一职责原则）
3. HttpConnector 实现Runnable接口，为后续多线程处理做铺垫





