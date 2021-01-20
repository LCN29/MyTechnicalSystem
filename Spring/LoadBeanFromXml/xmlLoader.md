# Spring Loader Bean From Xml File

## 1. 例子

````java

String xmlPath = "classpath*:${user.name}/spring-bean.xml";

// 1. 加载配置文件
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(xmlPath);
 
// 2. 从容器里面获取指定的bean 
Person person = (Person)context.getBean("personBean");
person.selfIntroduction();

````

## new ClassPathXmlApplicationContext(String xmlPath) 分析

`new ClassPathXmlApplicationContext(String xmlPath)` 内部会重新调用自身的重载构造函数 ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent);

```java
public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, @Nullable ApplicationContext parent) throws BeansException {

    super(parent);

    setConfigLocations(configLocations);
    if (refresh) {
        refresh();
    }
}
```

### 1. super(parent);
从

ClassPathXmlApplicationContext -> AbstractXmlApplicationContext -> AbstractRefreshableConfigApplicationContext -> AbstractRefreshableApplicationContext -> AbstractApplicationContext

调用到 AbstractApplicationContext 为止

```java
public AbstractApplicationContext(ApplicationContext parent) {
    this();
    setParent(parent);
}
```


1. 为 ClassPathXmlApplicationContext 初始解析




