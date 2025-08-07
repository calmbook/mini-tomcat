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

# 6.0
## 功能需求
1. 响应类添加标准响应头输出
2. 通过门面模式包装请求和响应

## 相关知识点
1. 关于Response响应头的输出时机（通过带缓存的OutputStream，在第一次flush的时候再发送），目前代码中未处理这一点
2. 门面模式包装，避免业务拿到对象之后强转，暴露内部的方法

# 7.0
## 功能需求
1. 解析url及请求体中获取请求参数
2. 实现Cookie及Session

## 相关知识点
1. url解析中，特殊字符的编码方式，例如空格编码为%20，特殊字符的编解码方式
   1. 编码规则：例如空格的ASCII值为32，转为十六进制就是0x20,所以编码后为字符串"%20"
   2. 解码规则：%20先将字符串20转换为数字2和数字0，再将十六进制的0x20转换为十进制的32（2 * 16 + 0，或者理解成0010左移4位变成0010 0000）
2. Cookie和Session的作用
   1. Cookie：由服务端创建，由客户端存储并在HTTP头中携带，主要用于解决HTTP无状态但又需要保持会话的需求(例如：登录认证后，其他请求可以通过cookie中携带的JSESSIONID获取session内容)
   2. Session，由服务端创建，服务端存储（用于存储会话信息），创建Session时会生成一个SessionID，通过设置Cookie，后续请求可以通过SessionID找到对应的会话状态

## 实现思路
1. 参数解析
   1. 使用到某个参数的时候再进行解析
   2. 解析过程分为url的参数解析和body中的参数解析（如果是GET请求无需处理body中的参数，目前只考虑POST）
   3. 特殊编码处理（例如以%开头的字符，是十六进制的字符，需要转换为十进制）
2. Cookie解析
   1. Cookie也是一个Header，只不过比较特殊，Cookie可以有多个值，存储为一个数组


# 8.0
## 功能需求
1. 现在每一个请求都是短连接，请求结束之后TCP连接就关掉了，连接的保持和关闭要求可以通过请求头进行控制
2. 分块传输（用于处理动态返回的内容，也就是在执行servlet之前无法确定返回内容的大小，但是静态内容，如文件图片是可以知道的，操作系统本身存储了相关的元数据）


### 实现效果
1. 用户在浏览器中输入`http://localhost:8080/servlet/test.TestServlet1?name=lw`
2. 页面返回"Hello"（实际写入的内容是 5\r\nHello\r\n0\r\n\r\n，返回内容符合分块传输的格式即可）
3. 通过wireshark捕获loopback traffic 筛选 `tcp.port == 8080`，可以观察到一次请求完成之后，请求并没有断开，并且TCP自身在发Keep-Alive心跳
4. 在浏览器中同一个标签页再次访问，可以看到并没有建立新的连接，而是在原有的连接上继续发送（可以通过四元组确定连接信息）
5. 在第3次访问时，服务端就会主动断开连接（在wireshark中也可以看到服务端主动发送FIN信号）

## 相关知识点
### Connection
以下内容均来自MDN
#### 概述
```
Connection 通用标头控制网络连接在当前会话完成后是否仍然保持打开状态。如果发送的值是 keep-alive，则连接是持久的，不会关闭，允许对同一服务器进行后续请求。
```
#### 语法
```
Connection: keep-alive
Connection: close
```
参数说明
- close
表明客户端或服务器想要关闭该网络连接，这是 HTTP/1.0 请求的默认值

- 以逗号分隔的 HTTP 头 [通常仅有 keep-alive]
表明客户端想要保持该网络连接打开，HTTP/1.1 的请求默认使用一个持久连接。这个请求头列表由头部名组成，这些头将被第一个非透明的代理或者代理间的缓存所移除：这些头定义了发出者和第一个实体之间的连接，而不是和目的地节点间的连接。

### Keep-Alive
以下内容均来自MDN
#### 概述
```
HTTP/1.0 默认在每次请求/响应交互后关闭连接，因此 HTTP/1.0 中的持久连接必须经过明确协商。一些客户端和服务器可能希望与以前的持久连接方式兼容，可以使用 Connection: keep-alive 请求标头来实现这一点。连接的其他参数可通过 Keep-Alive 标头请求。
```

#### 语法
`Keep-Alive: <parameters>`
参数说明
- timeout
指定了主机允许空闲连接保持打开状态的时长（以秒为单位的整数）。当主机没有接收或发送数据时，就认为连接是空闲的。主机可以保持连接超过 timeout 秒，但应该确保至少保持连接 timeout 秒。
- max
在此连接关闭之前，可以发送的请求的最大值。在非管道连接中，除了 0 以外，这个值是被忽略的，因为需要在紧跟着的响应中发送新一次的请求。HTTP 管道连接则可以用它来限制管道的使用。


#### 示例
```
HTTP/1.1 200 OK
Connection: Keep-Alive
Content-Encoding: gzip
Content-Type: text/html; charset=utf-8
Date: Thu, 11 Aug 2016 15:23:13 GMT
Keep-Alive: timeout=5, max=200
Last-Modified: Mon, 25 Jul 2016 04:32:39 GMT
Server: Apache

(body)
```

#### 处理流程
- 客户端/服务端均支持Keep-Alive
1. 请求头中带上Connection: Keep-Alive，表示客户端希望进行连接复用
2. 服务端如果支持连接复用，则会返回Connection: Keep-Alive，告诉客户端服务端支持此项功能（还可能返回超时时间以及请求数）
3. 服务端keep-alive超时或者请求数达到上线后关闭socket连接

- 服务不支持连接复用
1. 服务端不支持连接复用会忽略Connection: Keep-Alive，直接关闭socket连接

- 客户端不支持连接复用
1. 服务端默认开启了keep-alive，但是客户端不支持的话，客户端携带Connection:close
2. 服务端识别到Connection:close之后直接关闭连接


### 分块编码传输
分块编码主要应用于如下场景，即要传输大量的数据，但是在请求在没有被处理完之前响应的长度是无法获得的。例如，当需要用从数据库中查询获得的数据生成一个大的 HTML 表格的时候，或者需要传输大量的图片的时候。一个分块响应形式如下：

#### 示例
```
HTTP/1.1 200 OK
Content-Type: text/plain
Transfer-Encoding: chunked

7\r\n
Mozilla\r\n
11\r\n
Developer Network\r\n
0\r\n
\r\n
```

### TCP KeepAlive
服务端不关闭Socket连接，但是TCP自身会进行心跳探活，超时后客户端会发起关闭连接的请求

## 实现思路
当前版本暂时不处理超时断连以及最大请求数功能
1. 在request解析header时将connection的值（keep-alive or close）解析出来放入连接器中
2. 在processor处理时基于keepAlive判断是否需要关闭连接

ps: 当前版本中写死了传输格式 Transfer-Encoding: chunked（实际上应该根据返回内容的类型进行判断）


