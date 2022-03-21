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

2. 创建出一个 BeanDefinitionReader --> AnnotatedBeanDefinitionReader, 存放在 AnnotationConfigApplicationContext.reader   内部会同时会 BeanFactory 中注册了好几个后置处理器的 BeanDefinition

> ConfigurationClassPostProcessor BeanFactoryPostProcessor  主要用于解析 @ComponentScan @ComponentScans @Import 等注解
> AutowiredAnnotationBeanPostProcessor BeanPostProcessor 用于解析 @Autowired @Value 等注解
> CommonAnnotationBeanPostProcessor BeanPostProcessor 用于解析 @Component 和其子类 等注解
> PersistenceAnnotationBeanPostProcessor BeanPostProcessor
> EventListenerMethodProcessor BeanFactoryPostProcessor 和下面的 DefaultEventListenerFactory 一起处理 @EventListener 注解
> DefaultEventListenerFactory 普通的 Bean 类


3. 创建出一个 ClassPathBeanDefinitionScanner, 存放在 AnnotationConfigApplicationContext.scanner, ClassPathBeanDefinitionScanner 的核心就是里面的一个 doScan(String 包的根路径) 得到所有符合的 BeanDefinitionHold
   
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


```java
@Override
public void refresh() throws BeansException, IllegalStateException {

    synchronized (this.startupShutdownMonitor) {

        // 1. 调用 Environment 的 validateRequiredProperties 方法, 对必要的属性配置进行检查
        // 2. 初始 2 个时间监听器的列表 Set<ApplicationListener<?>> earlyApplicationListeners，Set<ApplicationListener<?>> applicationListeners 同时确保 applicationListeners 中是最全的
        // 3. 初始应用事件列表 Set<ApplicationEvent> earlyApplicationEvents, 这样可以在 multicaster 广播器初始之后, 里面的事件可以进行广播出去
        prepareRefresh();

        // 内部的代码就 2 行
        // refreshBeanFactory(); 调用到子类实现的方法, 在 AnnotationConfigApplicationContext 只是给自身的 BeanFactory 设置一个 id 
        // return getBeanFactory(); 同样调用到子类实现的方法, 在 AnnotationConfigApplicationContext 只是返回自身的 BeanFactory

        // 如果当前的 ApplicationContext 是 XML 的 ClassPathXmlApplicationContext
        // 1. 创建 DefaultListableBeanFactory 实例, 将其放到 AbstractRefreshableApplicationContext 的 beanFactory 属性
        // 2. 将 AbstractRefreshableConfigApplicationContext 中的 configLocations (XML 文件的路径) 加载为 Resource
        // 3. 通过 SAX XML 的解析方式, 从 Resource 中加载出 Bean 的定义 GenericBeanDefinition, 存入到 DefaultListableBeanFactory 的 Map<String, BeanDefinition> beanDefinitionMap, 
        // 存之前会判断 beanName 当前是否已经有已经创建的 bean
        // 4. 把 BeanName 对应的别名存储到 SimpleAliasRegistry 的 Map<String, String> aliasMap
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 设置 BeanFactory 的容器的参数

        // 1. 设置 AbstractBeanFactory 的 ClassLoader beanClassLoader 为当前的 ClassLoader
        // 2. 设置 AbstractBeanFactory 的表达式解析器 BeanExpressionResolver beanExpressionResolver 为 StandardBeanExpressionResolver
        // 3. 向 AbstractBeanFactory 的 Set<PropertyEditorRegistrar> propertyEditorRegistrars 添加一个 ResourceEditorRegistrar 实例

        // 4. 向 AbstractBeanFactory 的 List<BeanPostProcessor> beanPostProcessors 
        // 添加一个 ApplicationContextAwareProcessor 实例
        // 添加一个 ApplicationListenerDetector 实例

        // 4. 向 AbstractAutowireCapableBeanFactory 的 Set<Class<?>> ignoredDependencyInterfaces 添加 
        // EnvironmentAware
        // EmbeddedValueResolverAware
        // ResourceLoaderAware
        // ApplicationEventPublisherAware 
        // MessageSourceAware
        // ApplicationContextAware

        // 5. 向 AbstractAutowireCapableBeanFactory 的 Map<Class<?>, Object> resolvableDependencies 追加 
        // BeanFactory -  value 值为 入参的 ConfigurableListableBeanFactory 
        // ResourceLoader - value 值为当前所在类 AbstractApplicationContext 
        // ApplicationEventPublisher - value 值为当前所在类 AbstractApplicationContext 
        // ApplicationContext - value 值为当前所在类 AbstractApplicationContext 

        // 6. 如果当前的单例 bean 容器, 即 DefaultSingletonBeanRegistry 的 Map<String, Object> singletonObjects 中包含 beanName 为 loadTimeWeaver 的 bean, 则会
        // 向 AbstractBeanFactory 的 List<BeanPostProcessor> beanPostProcessors 添加一个 LoadTimeWeaverAwareProcessor 的实例
        // 设置当前的 BeanFacotry 的 TempClassLoader 为 ContextTypeMatchClassLoader

        // 7. 向 Spring 容器, 即 DefaultSingletonBeanRegistry 的 Map<String, Object> singletonObjects, 添加了 3 个 bean
        // beanName: environment, 对应的当前的 Environment,  
        // beanName: systemProperties, 对应的 Environment 的 SystemPropertie 属性
        // beanName: systemEnvironment, 对应的 Enviroment 的 SystemEnvironment 属性


        // 向 AbstractAutowireCapableBeanFactory 的 Set<Class<?>> ignoredDependencyInterfaces 中追加的接口作用, 可以看一下这里 https://www.jianshu.com/p/3c7e0608ff1f
        // 大体的作用如下：
        // 有个接口 I, 这个接口有个要实现的方法 void setA(A); 
        // 有个类 C 实现了接口 I, 里面有个属性 A a, 这个属性在 xml 或注解设置了自动注入, 同时实现了 I 接口， 属性 A 还可以通过 set 方法进行配置
        // 这时候把接口 I 放到了 ignoredDependencyInterfaces 中, 那么在属性自动配置的时候，就能进行跳过, 
        // 后面在 ApplicationContextAwareProcessor 手动通过 set 方法进行设置了, 这样可以防止同一个属性被多次设置

        // AbstractAutowireCapableBeanFactory 的 Map<Class<?>, Object> resolvableDependencies 追加属性
        // 大体的作用如下:
        // 在 Spring 注入中，默认是按类型注入的, 当出现同一个接口有多个实现时，那么就会出现注入失败
        // 可以通过这个指定某个类型，要注入的对象是什么
        prepareBeanFactory(beanFactory);

        try {

            // 空方法
            // 可以修改在标准初始化的 ApplicationContext 内部的 Bean Factory
            // 这个时候所有的 bean Definition 已经被加载了, 但是尚未有任何一个 bean 被实例化
            // 这里允许在具体的 ApplicationContext 实现类中注册特殊的 BeanPostProcessors 等
            postProcessBeanFactory(beanFactory);

            // 大部分的逻辑都在 PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors() 方法 
            // 主要是执行注册的 BeanFactoryPostProcessor 的方法

            // 1. 获取到 AbstractApplicationContext 中的 List<BeanFactoryPostProcessor> beanFactoryPostProcessors, 获取到的是已经实例化好的 BeanFacotryPostProcessor
            // 2. 遍历所有实例好的 BeanFactoryPostProcessor, 找出是 BeanDefinitionRegistryPostProcessor 的类型的, 同时调用他们的 postProcessBeanDefinitionRegistry 方法

            // 第一次查找
            // 3. 从容器中的 beanDefinitionMap 中获取同时实现了 BeanDefinitionRegistryPostProcessor 和 PriorityOrdered 的类的 beanName，找到并获取实例, 这时获取到的是 BeanDefinition 中的 bean, 原本未实例化的
            // 4. 将找到的 BeanDefinitionRegistryPostProcessor 列表按照配置的 OrderComparator 进行排序
            // 5. 遍历排序好的 BeanDefinitionRegistryPostProcessor 的列表, 调用其 postProcessBeanDefinitionRegistry 方法

            // 第二次查找
            // 6. 从容器中的 beanDefinitionMap 中获取同时实现了 BeanDefinitionRegistryPostProcessor 和 Ordered 的类的 beanName，找到并获取实例, 这时获取到的是 BeanDefinition 中的 bean, 原本未实例化的, 也可能是第一次中实例好的
            // 7. 将找到的 BeanDefinitionRegistryPostProcessor 列表, 过滤掉第一次查找中已经调用的 (存在同时实现 PriorityOrdered 和 Ordered 的), 按照配置的 OrderComparator 进行排序
            // 8. 遍历排序好的 BeanDefinitionRegistryPostProcessor 的列表, 调用其 postProcessBeanDefinitionRegistry 方法

            // 第三次查找
            // 9. 从容器中的 beanDefinitionMap 中获取实现了 BeanDefinitionRegistryPostProcessor 类的 beanName，找到并获取实例, 这时获取到的是 BeanDefinition 中的 bean, 原本未实例化的, 也可能是第一, 二次中实例好的
            // 10. 将找到的 BeanDefinitionRegistryPostProcessor 列表, 过滤掉第一, 二次查找中已经调用的, 按照配置的 OrderComparator 进行排序
            // 11. 遍历排序好的 BeanDefinitionRegistryPostProcessor 的列表, 调用其 postProcessBeanDefinitionRegistry 方法

            // 补充查找
            // 12. 因为存在 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 动态创建 BeanDefinitionRegistryPostProcessor 的 BeanDefinition 的情况, 所以需要再重复第三次查找
            // 确保最终容器中的 beanDefinitionMap 的 BeanDefinitionRegistryPostProcessor 都是实例好, postProcessBeanDefinitionRegistry 方法都是执行过的

            // 13 将上面的上面执行过 postProcessBeanDefinitionRegistry 方法的 BeanDefinitionRegistryPostProcessor 类, 依次执行其 postProcessBeanFactory 方法
            // 14 调用 AbstractApplicationContext 中的 List<BeanFactoryPostProcessor> beanFactoryPostProcessors 中剩余没有调用过 postProcessBeanFactory 方法的 BeanPostProcessor 的 postProcessBeanFactory 方法

            // 按照上面的第一，二，三次查找的方式, 处理容器中的 beanDefinitionMap 的还未处理的 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法

            // 总结：
            // 将 BeanDefinitionRegistryPostProcessor 按照
            // AbstractApplicationContext 中的 List<BeanFactoryPostProcessor> 已经初始化的
            // 实现 PriorityOrdered
            // 实现 Ordered 
            // 没有任何实现 Ordered 的类型
            // 的顺序和其内部自行在排一次序, 得到最终的顺序进行执行 postProcessBeanDefinitionRegistry 方法
            // 再将 BeanFactoryPostProcessor 按照实现同样的逻辑进行排序后，顺序执行 postProcessBeanFactory 方法
            invokeBeanFactoryPostProcessors(beanFactory);


            registerBeanPostProcessors(beanFactory);
            initMessageSource();
            initApplicationEventMulticaster();
            onRefresh();
            registerListeners();
            finishBeanFactoryInitialization(beanFactory);
            finishRefresh();

        } catch (BeansException ex) {

            destroyBeans();
            cancelRefresh(ex);
            throw ex;

        } finally {
            resetCommonCaches();
        }

    }


}
```

## 3 内置后置处理器



## 问题

ApplicationContext 和 BeanFactory 有什么区别

ApplicationContext 实现了 BeanFactory, 所以 2 者都具备了生产 Bean 的功能
而 ApplicationContext 在获取 Bean 的基础上, 丰富了更多的功能, 比如包扫描，解析, 国际化, Spring 容器生命周期等

2 者都能作为 Bean 的容器，
但是 BeanFactory 只能手动的一个一个的注册 BeanDefinition
而 ApplicationContext 提供了批量的方式, 比如配置文件，指定配置类