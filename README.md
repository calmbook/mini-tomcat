# mini-tomcat
从零开始写一个简易版本的tomcat

# 参考资料
https://gitee.com/wang-kh/minit-learning-demo/

# 1.0
## 功能需求
静态资源服务器，用户在浏览器输入下面这个地址
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
