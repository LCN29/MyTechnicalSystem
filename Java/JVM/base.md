# JVM

Java 文件  ---> 编译为 Java Class 文件(16进制文件) ----> 将 Class 文件放到 JVM 中

 class 文件 u --> 2 位     cafe babe 表示 u4


 类加载机制  ---> class 文件 放到 JVM 中

 1. 装载
 >>1. 找到类文件所在的位置(磁盘，全路径)  ----> 类装载器(ClassLoader) ----> 寻找
 >>2. 类文件的信息交给JVM
 >>3. 类文件所对应的 Class 对象

 2. 链接
 >>1. 验证
 >>2. 准备
 >>3. 解析

 3. 初始化



1. 装载

通过 ClassLoader 进行加载
区分  Bootstrap ClassLoader   Extension ClassLoader   App ClassLoader   Custom ClassLoader 

装载机制
双亲委派  ---> 加载一个类，先让顶级的 Loader 进行加载尝试, 加载到，自身不加载，加载不到，顺着下一级的 Loader

破坏双亲委派