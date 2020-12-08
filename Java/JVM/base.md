# JVM

Java 文件  ---> 编译为 Java Class 文件(16进制文件) ----> 将 Class 文件放到 JVM 中

 class 文件 u --> 2 位     cafe babe 表示 u4


 类加载机制  ---> class 文件 放到 JVM 中

 1. 装载
 >>1. 找到类文件所在的位置(磁盘，全路径)  ----> 类装载器(ClassLoader) ----> 寻找
 >>2. 类文件的信息交给JVM  ---> 类文件字节码流静态存储结构 ---> JVM 里面的某个区域 【Method Area 方法区】 
 >>3. 类文件所对应的 Class 对象 ---> 存储区域 堆 【heap】

 2. 链接
 >>1. 验证 保证被加载的类的准确性
 >>2. 准备 为类的静态变量分配内存空间，并将其值初始化为默认值 staic int a = 0;
 >>3. 解析 将类中的符号引用转换为直接引用  String str = 地址是什么

 3. 初始化 为静态变量赋予真正的值 staic int a = 10



1. 装载


Boostrap ClassLoader --> $Java_Home$ 中 jre/lib/rt.jar 中所有 class 或 Xbootclasspath 选项指定的 jar 包

Extension ClassLoader --> 加载 Java 平台中扩展功能的一些 Jar 包, 包括 $Java_Home 中 `jre/lib/ext/*.jar` 或 -Djava.ext.dirs 指定目录下的 jar 包


App ClassLoader --> 加载 classpath 中指定目录下的类和 jar 包

Custom ClassLoader --> 通过 java.lang.ClassLoader 的子类自定义加载 class, 属于应用程序根据自身需要自定义的 ClassLoader ,如 tomcat ,jboss 都会根据 j2ee 规范自行实现 ClassLoader

通过 ClassLoader 进行加载
区分  Bootstrap ClassLoader   Extension ClassLoader   App ClassLoader   Custom ClassLoader 

装载机制
双亲委派  ---> 加载一个类，先让顶级的 Loader 进行加载尝试, 加载到，自身不加载，加载不到，顺着下一级的 Loader

破坏双亲委派

重写 ClassLoader 的 loadClass 方法

```java
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

	synchronized (getClassLoadingLock(name)) {

		// 首先检查这个classsh是否已经加载过了
		Class<?> c = findLoadedClass(name);

		if (c == null) {

			long t0 = System.nanoTime();
            try {
                // c==null表示没有加载，如果有父类的加载器则让父类加载器加载
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    //如果父类的加载器为空 则说明递归到bootStrapClassloader了
                    //bootStrapClassloader比较特殊无法通过get获取
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {}

            if (c == null) {
            	//如果bootstrapClassLoader 仍然没有加载过，则递归回来，尝试自己去加载class
                long t1 = System.nanoTime();
                c = findClass(name);
                sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                sun.misc.PerfCounter.getFindClasses().increment();

            }
		}

		if (resolve) {
	        resolveClass(c);
	    }
	    return c;
	}

}
```


https://www.jianshu.com/p/1e4011617650

https://www.jianshu.com/p/7d12d8697fd1