# 1 Throwable


如图:   

![Alt 'JavaThrowableRelationClass'](https://raw.githubusercontent.com/PictureRespository/Java/main/Throwable/JavaThrowableRelationClass.png)

展示了 Java 整个异常体系的关系。

Throwable 的 Java 异常体系的基类, 他的直接子类有 Error 和 Exception 2 个。  

## 1.1 Error
其中 Error 表示的是由于系统错误, Java 虚拟机抛出的异常, 例如 Java 虚拟机崩溃, 内存不够等, 这种情况仅凭程序自身是无法处理的, 在程序中也不会对 Error 异常进行捕捉和抛出。   

常见的实现有
> 1. OutOfMemoryError
> 2. StackOverflowError
> 3. IOError

## 1.2 Exception

Exception 一般是由程序逻辑错误引起的，需要从程序逻辑角度进行处理, 尽可能避免这类异常的发生。 总体可以细分为 2 类 RuntimeException (运行时异常) 和 非 RuntimeException (非运行时异常), 
也叫做 CheckedException (检查时异常)。

### 1.2.1 RuntimeException

程序运行过程中才可能发生的异常, 一般为代码的逻辑错误: 空指针, 类型错误转换，数组下标访问越界，网络端口被占用等。 这里异常在代码编译期是无感知, 无法检查出来的, 只有在实际运行代码时则会暴露出来。

常见的实现有
> 1. ArrayIndexOutOfBoundsException
> 2. ClassCastException
> 3. NullPointerException

### 1.2.2 非 RuntimeException (CheckedException)

编译期间可以检查到的异常, 必须显式的进行处理
> 1. 通过 try - catch 进行捕获处理
> 2. 通过 throw - throws 抛出给上一层

常见的实现有  
> 1. IOException
> 2. InterruptedException
> 3. NoSuchMethodException