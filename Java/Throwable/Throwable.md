# Throwable


Throwable Java 异常体系的基类, 他的直接实现有 Error 和 Exception 2 个。  

其中 Error 表示的是由于系统错误, Java 虚拟机抛出的异常, 例如 Java 虚拟机崩溃等, 这种情况仅凭程序自身是无法处理的, 在程序中也不会对 Error 异常进行捕捉和抛出。 

而 Exception 可以细分为 RuntimeException (运行时异常) 和 CheckedException (检查时异常), 一般是由程序逻辑错误引起的，程序应该从逻辑角度尽可能避免这类异常的发生
* RuntimeException : 程序运行过程中才可能发生的异常。一般为代码的逻辑错误。例如：类型错误转换，数组下标访问越界, 空指针异常、找不到指定类等等
* CheckedException : 编译期间可以检查到的异常, 必须显式的进行处理（捕获或者抛出到上一层）。 例如: IOException, FileNotFoundException等等
