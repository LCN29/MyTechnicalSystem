# NIO

通过一种机制，可以监视多个描述符，一旦某个描述符就绪(一般是读就绪或者写就绪), 能够通知程序进行相应的读写操作。


系统级别的实现
select
poll
epoll

select的几大缺点：
（1）每次调用select，都需要把fd集合从用户态拷贝到内核态，这个开销在fd很多时会很大
（2）同时每次调用select都需要在内核遍历传递进来的所有fd，这个开销在fd很多时也很大
（3）select支持的文件描述符数量太小了，默认是1024


https://blog.csdn.net/coolgw2015/article/details/79719328