# Spring 

## 1 Spring 中的简单了解

```java
public class Test {

    public static void main(String[] args){
        
        // 第一步
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("XML 配置文件路径");
        // AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("配置类");
        
        // 第二步
        Person person = (Person)context.getBean("personBean");
    }
}
```

### BeanFactory

``context.getBean("beanName")`` 内部的实现 --> ``getBeanFactory().getBean(name)``

BeanFactory Spring 的顶层接口类, 通过简单工厂模式, 负责生产 bean。
BeanFactory 这个接口声明的方法, 都是和 bean 有关的, 比如通过 beanName, classType 获取对应的实例 bean, 判断 bean 是否存在等。

简单模式  
由一个工厂类根据传入的参数, 动态的决定创建哪一个产品类

### BeanDefinition

配置输入项, 如 xml, Java 配置类
在 Context 会先将这些配置统一解析为 BeanDefinition, 内部包含了这个类的各种定义, 属性名和对应的值, 是否单例, 销毁方法, 初始方法等
后面工厂在根据这些 BeanDefinition 产生出对应的 bean
等。

BeanDefinition Spring 顶层接口类, 封装了生产 Bean 所需要的一切原料, 是否单例, 如何注入, 属性等


### BeanDefinitionRegistry

BeanDefinitionRegistry 将 BeanDefinition 注册到一个容器, 也就是 Map 中, 后续需要的 bean 就可以直接读取这个容器, 进行生产

### BeanDefinitionReader

BeanDefinitionReader 从 xml 配置或者 Java 配置类等读取出 BeanDefinition

### ClassPathBeanDefinitionScanner
如果是基于注解的方式的话, 还有一个 ClassPathBeanDefinitionScanner 类需要了解一下。
一般通过注解的方式进行注册时, 都需要配置一个根目录, 比如 `com.lcn29.code`, 表示这个目录下的类, 需要进行扫描, 对有需要的类, 比如注解了 
@Component 对齐进行注册, 本身 100 个类里面可以就只需要 10 个类需要进行注册的。

这个类就是用来扫描根目录下需要注册的类, 可以理解为就是一个过滤出需要的类。

BeanDefinitionReader 读取配置类
ClassPathBeanDefinitionScanner  扫描出需要注册的类
BeanDefinitionRegister 对 BeanDefinition 进行注册到容器


### Bean 的加载过程

1. 通过 BeanDefinition 反射生成实例, 实例化 (反射或者工厂方法, 也就是 FactoryBean 接口, 前者由Spring进行控制, 后者由用户自定义)
2. 填充属性 @Value @Autowired 
3. 初始化， initMethod 
4. 放到一个 Map (单例池, 一级缓存), key 为 beanName, value bean 实例


### 扩展点

BeanFactory 后置处理器,
BeanFactoryPostProcessor, 需要实现的方法的参数只有 1 个 BeanFactory,
以通过 BeanFactory 获取到对应的 BeanDefinition, 并对其进行修改

其本身还有一个扩展子接口 BeanDefinitionRegistryPostProcessor,
在 BeanFactoryPostProcessor 动态修改的基础上, 追加了一个方法, 可以用来动态注册其他的 BeanDefinition

Bean 的扩展点 BeanPostProcessor
会在 bean 的生命周期中各个阶段都可能调用到

## 2 IOC 源码

```java
@ComponentScan(basePackages = "com.can.spring.core") 
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationContext.class);
        Person personBean = (Person) context.getBean("person");
        personBean.selfIntroduction();
    }
}
```

### 流程

1. 创建出一个 BeanFactory --> DefaultListableBeanFactory, 存放在 GenericApplicationContext.beanFactory

2. 创建出一个 BeanDefinitionReader --> AnnotatedBeanDefinitionReader, 存放在 AnnotationConfigApplicationContext.reader
内部会同时会 BeanFactory 中注册了好几个后置处理器的 BeanDefinition
> ConfigurationClassPostProcessor BeanFactoryPostProcessor  主要用于解析 @ComponentScan @ComponentScans @Import 等注解
> AutowiredAnnotationBeanPostProcessor BeanPostProcessor 用于解析 @Autowired @Value 等注解
> CommonAnnotationBeanPostProcessor BeanPostProcessor 用于解析 @Component 和其子类 等注解
> PersistenceAnnotationBeanPostProcessor BeanPostProcessor
> EventListenerMethodProcessor BeanFactoryPostProcessor 和下面的 DefaultEventListenerFactory 一起处理 @EventListener 注解
> DefaultEventListenerFactory 普通的 Bean 类


3. 创建出一个 ClassPathBeanDefinitionScanner, 存放在 AnnotationConfigApplicationContext.scanner  
   ClassPathBeanDefinitionScanner 的核心就是里面的一个 doScan(String 包的根路径) 得到所有符合的 BeanDefinitionHold
   
4. 调用 reader.register(Class<?> configClass) 进行 BeanDefinitionHold 的获取
5. 进入到 AbstractApplicationContext.refresh 方法, Spring 的核心方法

### refresh 方法流程

正常的方法流程
> prepareRefresh()
> ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory()
> prepareBeanFactory(beanFactory);
> postProcessBeanFactory(beanFactory);
> invokeBeanFactoryPostProcessors(beanFactory); 实例化 BeanFactoryPostProcessor, 从 BeanDefinition 转为 Bean
> registerBeanPostProcessors(beanFactory); 实例化 BeanPostProcessor,
> initMessageSource();
> initApplicationEventMulticaster();
> onRefresh();
> registerListeners();
> finishBeanFactoryInitialization(beanFactory); 注册单例 bean
> finishRefresh();
> resetCommonCaches()

## 3 内置后置处理器



## 问题

ApplicationContext 和 BeanFactory 有什么区别

ApplicationContext 实现了 BeanFactory, 所以 2 者都具备了生产 Bean 的功能
而 ApplicationContext 在获取 Bean 的基础上, 丰富了更多的功能, 比如包扫描，解析, 国际化, Spring 容器生命周期等

2 者都能作为 Bean 的容器，
但是 BeanFactory 只能手动的一个一个的注册 BeanDefinition
而 ApplicationContext 提供了批量的方式, 比如配置文件，指定配置类