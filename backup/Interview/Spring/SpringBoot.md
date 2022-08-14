# SpringBoot

## 1. SpringBoot 的正常关闭

### 1.1. 借助 SpringBoot 提供的 actuator starter

1. 在 pom.xml 文件追加这个依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. 暴露出 /shutdown 节点

```
management.endpoint.shutdown.enabled=true
management.endpoints.web.exposure.include=shutdown
```

3. 调用接口促使服务结束

```
curl -X POST http://host:port/应用的其他路径配置, 如果有的话/actuator/shutdown
```

### 1.2. 在 Linux/Unix 中将当前的 Java 应用注册为 

1. 在 pom.xml 文件中追加

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <!-- 添加这个配置 -->
        <executable>true</executable>
    </configuration>
</plugin>
```

2. 为服务建立软链接

```sh
sudo ln -s /SpringBoot打包的成的Jar包路径 /etc/init.d/应用的别名
```

3. 授权

```sh
chmod +x /SpringBoot打包的成的Jar包路径
```

4. 通过 service 控制应用

```sh
sudo service 应用别名 start|stop
```

### 1.3. 通过 kill -15

1. 先发送请求到 eruka 通知应用下线
2. 脚本沉睡 90s, 等待其他节点感知到
3. 通过 kill -15 正常的关闭程序


### 1.4. 通过进程 ID 结束
1. 在 SpringBoot 启动的时候将进程号写入一个 app.pid 文件
2. 然后通过 cat /app.id 文件所在的路径 | xargs kill 命令直接停止服务




## 2. SpringBoot 关闭时, 如何进行中的任务的执行完成

1. 当前的 Tomcat 服务器不在接收新的请求, 已有的请求处理完
2. 线程池关闭, 已有和进行中的任务处理

实现原理: 
> 1. SpringBoot 在启动的时候会向 Java 虚拟机注册一个钩子
> 2. 在钩子函数中, 大体会执行 发送出通知关闭事件给所有的事件监听器, 销毁 bean 等操作
> 3. 发送出 ContextClosedEvent 事件时, 事件广播器, 内部支持定义一个线程池, 在有线程池的情况下, 通过线程池将事件交由事件监听者处理, 没有线程池, 当前线程遍历交给事件监听者处理
> 4. 默认情况下, 都是没有线程池配置的, 所以在事件广播中, bean 还没销毁, 完善可以用的, 这时注册一个监听 ContextClosedEvent 的监听器, 在内部处理结束时的任务即可
> 5. 直接注册钩子函数, 无法保证注册的钩子函数和 SpringBoot 注册的钩子函数的执行顺序, 如果任务的处理需要依赖某些 bean, 那么会很麻烦
> 6. Tomcat 自身的行为可以通过 TomcatConnectorCustomizer 接口进行处理, 自定义 Tomcat Connector 行为
> 7. Connector 属于 Tomcat 抽象组件，功能就是用来接受外部请求，以及内部传递，并返回响应内容，是 Tomcat 中请求处理和响应的重要组件
> 8. 也就是说通过获取 Connector 就可以自定义 Tomcat 的请求行为

综上有

1. 创建监听类
```java
public class MyCloseService implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

    private volatile Connector connector;

    @Override
    public void customize(Connector connector) {
        this.connector = connector;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        // 暂停接收外部的其他请求
        this.connector.pause();
        // 获取 Tomcat 内部的线程池
        Executor executor = this.connector.getProtocolHandler().getExecutor();

        if (executor instanceof ThreadPoolExecutor) {
            try {
                
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                // shutdown 线程池不在接收其他任务, 会把处理中的和队列中的任务处理完
                threadPoolExecutor.shutdown();
                // 阻塞等待多少秒, 如果返回 false 表示还是没有处理完成
                if (!threadPoolExecutor.awaitTermination(waitTime, TimeUnit.SECONDS)) {
                    log.warn("Tomcat thread pool did not shut down gracefully within " + waitTime + " seconds. Proceeding with forceful shutdown");
                }

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        // 自定义线程池, 可以通过 shutdownNow() 将进行中的认为强制结束, 返回全部为执行的任务, 获取到后, 自行处理
    }
}
```

2. 把上面的类, 在配置类中 @Bean 注解注入 Spring 容器中
3. 在 配置文件中获取 TomcatServletWebServerFactory, 或者手动创建,
4. 通过 TomcatServletWebServerFactory.addConnectorCustomeizes(自己定义的MyCloseService 实例), 进行注册



