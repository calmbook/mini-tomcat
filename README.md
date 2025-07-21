[TOC]

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


# 4.0 
## 功能需求
提升processor处理的性能

用户在浏览器打开多个标签页，输入下面这个地址
   ```
      http://localhost:8080/servlet/test.HelloServlet
   ```
业务逻辑执行30s后，多个标签页均返回当前时间，并且应该几乎是同时返回的


## 实现流程
1. processor池化，减少对象创建及初始化的开销
   极简版本池化，不考虑对象的销毁
2. processor异步化处理，目前读取socket，处理业务这2个动作是同步进行的，处理业务时不能进行接收读取新的连接请求
   通过异步化处理，将HTTP连接建立以及业务处理请求的线程分开

   这要求业务线程处理完成之后通知主线程，涉及到线程同步（等待-通知）
### processor 池
1. 初始化（最小数量初始化），使用队列进行存储(应该使用哪种数据结构进行存储呢？存取的方式应该是什么策略？)
2. 分配与归还（池中有就从池里拿，没有就临时新建一个，直至达到最大线程数量之后，拒绝新的业务请求）
   注意是临时新建，即不存入池中，没有做线程池那种基于存活时间销毁的功能


### processor异步处理
1. processor 实现Runnable接口，初始化时start启动线程，while循环一直等待socket连接

   为什么不用JDK的线程池（因为线程池核心线程池满了之后，默认会进入队列等待，I/O密集型的应用实际上没必要等待，有突发流量的时候直接创建新的线程
响应速度）
2. connector 不再直接调用processor的线程池，仅仅传入socket连接后就返回，从而可以同时处理多个http请求
3. processor 和 connector之间的线程同步通过wait-notify加一个状态位控制，详见HttpProcessor注释说明   

## 相关知识点
1. 池化思想
2. 异步化处理
3. 线程同步机制，wait-notify（需要加上while循环，避免虚假唤醒）

# 5.0
## 功能需求
进一步引入规范
1. 引入HttpServletRequest和HttpServletResponse接口，进一步规范化
2. 解析请求行（请求的第一行）和请求头

## 实现思路
1. 字符串整体解析思路就是从InputStream中不断读取每一个字节，根据HTTP规范
通过换行符，空格等关键分隔符解析请求头中的内容，相比于之前的字符串整体读取出来之后
再通过正则匹配分割效率更高






