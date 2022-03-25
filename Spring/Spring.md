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

            // 15 调用 beanFactory 的 clearMetadataCache
            // 15.1 清除 AbstractBeanFactory 中的 Map<String, RootBeanDefinition> mergedBeanDefinitions> 中在上面已经实例过的 BeanDefintion
            // 15.2 清空 DefaultListableBeanFactory 中的 Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders
            // 15.3 清空 DefaultListableBeanFactory 中的 Map<Class<?>, String[]> allBeanNamesByType
            // 15.4 清空 DefaultListableBeanFactory 中的 Map<Class<?>, String[]> singletonBeanNamesByType

            // 总结：
            // 将 BeanDefinitionRegistryPostProcessor 按照
            // AbstractApplicationContext 中的 List<BeanFactoryPostProcessor> 已经初始化的
            // 实现 PriorityOrdered
            // 实现 Ordered 
            // 没有任何实现 Ordered 的类型
            // 的顺序和其内部自行在排一次序, 得到最终的顺序进行执行 postProcessBeanDefinitionRegistry 方法
            // 再将 BeanFactoryPostProcessor 按照实现同样的逻辑进行排序后，顺序执行 postProcessBeanFactory 方法

            // BeanDefinitionRegistryPostProcessor 偏重于 BeanDefinition 的注册, 优先级比较高
            // BeanFactoryPostProcessor            偏重于 BeanDefinition 的修改
        
            // 基于注解的 Spring BeanDefinition 的扫描都是通过 ConfigurationClassPostProcessor 这个 BeanDefinitionRegistryPostProcessor 实现的
        
            invokeBeanFactoryPostProcessors(beanFactory);

            // 向 BeanFactory 注册 BeanPostProcessors, 创建 BeanPostProcessors 的实例

            // 1. 直接先注册一个 BeanPostProcessorChecker 的 BeanPostProcessor 到 AbstractBeanFactory 的 List<BeanPostProcessor> beanPostProcessors 集合
            // BeanPostProcessorChecker 打印一条记录，内容为：在 BeanPostProcessors 实例化期间创建的 bean, 这些 bean 会不受 BeanPostProcessors 的影响

            // 2. 从 BeanFactory 中获取所有实现 BeanPostProcessor 和 PriorityOrdered 的 BeanDefinition, 通过 getBean 方法实例化
            // 3. 对所有实现 BeanPostProcessor 和 PriorityOrdered 的 Bean 进行排序并添加到 AbstractBeanFactory 的 List<BeanPostProcessor> beanPostProcessors 集合
            // List 是有序的, 所以上一步的排序是有意义的, 在调用 AbstractBeanFactory 的方法向 List<BeanPostProcessor> 添加时, 会判断一次添加的项是否存在, 存在先删除, 然后添加, 达到了新加入的项在末尾

            // 4. 重复上面的流程，处理掉实现 Order 的 BeanPostProcessor 和没有实现任何 Order 的 BeanPostProcessor, 排序后添加到 List<BeanPostProcessor> beanPostProcessors 集合
            // 5. 再次遍历所有的 beanName 列表，找出所有实现 MergedBeanDefinitionPostProcessor 的 bean, MergedBeanDefinitionPostProcessor 是 BeanPostProcessor 的扩展接口
            // 6. 对这些 Spring 内部的 BeanPostProcessor 的 MergedBeanDefinitionPostProcessor 集合排序, 然后添加到 List<BeanPostProcessor> beanPostProcessors 集合中
            // 7. 在自动地向容器中添加一个 ApplicationListenerDetector 的 BeanPostProcessor
            // 在 BeanFactory 创建的过程中，在 prepareBeanFactory 方法中已添加了一个 ApplicationListenerDetector 的 BeanPostProcessor
            // 经过 7 的操作, 原本旧的 ApplicationListenerDetector 会被删除, 然后追加了一个 ApplicationListenerDetector 到 List<BeanPostProcessor> 容器的最后一位

            registerBeanPostProcessors(beanFactory);
            
            // 确保有 beanName 为 messageSource 的单例 bean
            
            // 1. 判断 (已创建处理的单例是否包含 beanName 为 messageSource 的实例 ||  beanDefinitionMap 包含 messageSource) && (messageSource 不是引用 || messageSource 对应的 bean 不是 FactoryBean 的子类)
            // 1.1 如果为 true, 通过 getBean 方法, 确保 messageSource 这个 beanName 对应的 bean 存在
            // 1.2 如果为 false, 创建 DelegatingMessageSource 实例，尝试设置其 parentMessageSource 为 beanFacotory 的 parent, 然后把这个实例放到 Spring IOC 容器中, beanName 为 messageSource
            initMessageSource();

            // 确保有 beanName 为 applicationEventMulticaster 的 ApplicationEventMulticaster 的单例 bean, 这个是事件广播器

            // 1. 判断 (已创建处理的单例是否包含 beanName 为 applicationEventMulticaster 的实例 ||  beanDefinitionMap 包含 applicationEventMulticaster) && (applicationEventMulticaster 不是引用 || applicationEventMulticaster 对应的 bean 不是 FactoryBean 的子类)
            // 1.2 如果为 true, 通过 getBean 方法, 确保 applicationEventMulticaster 这个 beanName 对应的 bean 存在
            // 1.3 如果为 false, 创建 SimpleApplicationEventMulticaster, 然后把这个实例放到 Spring IOC 容器中, beanName 为 applicationEventMulticaster

            initApplicationEventMulticaster();

            // 空方法
            // 子类可以重写这个方法, 进行处理
            onRefresh();
            
            // 注册事件监听器

            // 1. 从 BeanFactory 的 Set<ApplicationListener<?>> applicationListeners 获取已经注册在容器中的监听器
            // 2. 把第一步获取到的 ApplicationListener 添加到容器当前的 ApplicationEventMulticaster 的内部的事件监听器集合中
            // 3. 从 BeanFactory 中获取所有 ApplicationListener 的实现类的 beanName 集合
            // 4. 遍历第三步获取到的 beanName 集合, 添加到 ApplicationListener 的 ListenerRetriever 的 Set<String> applicationListenerBeans 中
            // 5. 获取早期注册在 BeanFactory 的 Set<ApplicationEvent> earlyApplicationEvents 列表
            // 6. 置空 earlyApplicationEvents
            // 7. 遍历第五步获取到的 ApplicationEvent 列表, 调用 ApplicationEventMulticaster.multicastEvent 方法，广播给当前的所有的事件监听器 Listener
            
            registerListeners();

            // 实例化剩下的所有非 lazy-init 的单例 bean
            
            // 1. 判断 (已创建处理的单例是否包含 beanName 为 conversionService 的实例 || beanDefinitionMap 包含 conversionService 的 beanName)
            // 1.2 如果有, 判断这个 beanName 的 class 是否为 ConversionService 的实现类
            // 1.2.1 是的话, 通过 getBean 确保这个 bean 已经存在, 同时设置 BeanFactory 的 ConversionService 为这个 bean
            
            // 2. BeanFactory 的 List<StringValueResolver> embeddedValueResolvers 是否为空, 
            // 2.1 如果为空的话, 填充一个默认的实现 (str)-> strVal -> getEnvironment().resolvePlaceholders(strVal), 占位符解析器
            
            // 3. 从 BeanFactory 中获取实现 LoadTimeWeaverAware 的 beanName 列表
            // 4. 遍历获取到的 beanName 列表, 通过 getBean 方法确定这个 bean 已经存在
            
            // 5. 设置 BeanFactory 的 TempClassLoader 为 null
            // 6. 设置 BeanFactory 的 configurationFrozen 属性为 true, 标识后面的配置冻结了, 不允许修改
            // 7. 把当前所有的 beanDefinitionName 转为数组, 放到 BeanFactory 的 frozenBeanDefinitionNames
            // 8. 调用 BeanFactory 的 preInstantiateSingletons 实例化剩余的 bean, 进入 bean 的解析
            finishBeanFactoryInitialization(beanFactory);

            // context 初始的收尾

            // 1.  clearResourceCaches(), 清除 resource 资源的缓存
            // 1.1 DefaultResourceLoader 中的 Map<Class<?>, Map<Resource, ?>> resourceCaches 清空

            // 2. initLifecycleProcessor(), 初始生命周期处理器
            // 2.1  判断 (已创建处理的单例是否包含 beanName 为 lifecycleProcessor 的实例 ||  beanDefinitionMap 包含 lifecycleProcessor) && (lifecycleProcessor 不是引用 || lifecycleProcessor 对应的 bean 不是 FactoryBean 的子类)
            // 2.1.1 为 true,  尝试从通过 getBean 方法从容器中获取 lifecycleProcessor 且类型为 LifecycleProcessor 的 bean, 设置到当前 Application 的 LifecycleProcessor lifecycleProcessor, 可能为空
            // 2.1.2 为 false, 设置当前 Application 的 LifecycleProcessor lifecycleProcessor 等于 DefaultLifecycleProcessor, 并且将这个实例添加到 Spring 的 IOC 中, beanName 为 lifecycleProcessor

            // 3. getLifecycleProcessor().onRefresh() 调用当前 Application 的 LifecycleProcessor lifecycleProcessor 的 onRefresh 方法
            // 3.1 获取容器中注册的 Lifecycle 类型的 bean,  遍历所有的 bean
            // 3.2 这个 bean 是 SmartLifecycle， 设置了允许自动执行, 继续，按照自身设置的执行优先度, 排序后，执行执行
            // 3.3 设置当前的 lifecycleProcessor 的 running 为 true

            // 4. publishEvent(new ContextRefreshedEvent(this)), 广播出一个  ContextRefreshedEvent 事件

            // 5. LiveBeansView.registerApplicationContext(this), 调用 LiveBeansView 的 registerApplicationContext
            // 5.1 如果环境中配置了 spring.liveBeansView.mbeanDomain 属性值
            // 5.1.1 添加当前的 ConfigurableApplicationContext applicationContext 到 Set<ConfigurableApplicationContext> applicationContexts 中
            // 作为一个当前运行的快照, 后面可以通过这个输出为 json 字符串等

            finishRefresh();

        } catch (BeansException ex) {

            destroyBeans();
            cancelRefresh(ex);
            throw ex;

        } finally {

            // 1. ReflectionUtils.clearCache(), 清除 Map<Class<?>, Method[]> declaredMethodsCache 和 Map<Class<?>, Field[]> declaredFieldsCache 的缓存
            
            // 2. AnnotationUtils.clearCache(), 清除下面的缓存
            // 2.1 Map<AnnotationCacheKey, Annotation> findAnnotationCache
            // 2.2 Map<AnnotationCacheKey, Boolean> metaPresentCache
            // 2.3 Map<AnnotatedElement, Annotation[]> declaredAnnotationsCache
            // 2.4 Map<Class<? extends Annotation>, Boolean> synthesizableCache
            // 2.5 Map<Class<? extends Annotation>, Map<String, List<String>>> 
            // 2.6 Map<Class<? extends Annotation>, List<Method>> attributeMethodsCache
            // 2.7 Map<Method, AliasDescriptor> aliasDescriptorCache 

            // 3. ResolvableType.clearCache(), 清除 ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache 和 SerializableTypeWrapper 的 ConcurrentReferenceHashMap<Type, Type> cache

            // 4. CachedIntrospectionResults.clearClassLoader(getClassLoader())
            // 4.1 从 Set<ClassLoader> acceptedClassLoaders 从中清除当前的 ClassLoader 
            // 4.2 从 ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache  从中清除当前的 ClassLoader 
            // 4.3 从 ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache 从中清除当前的 ClassLoader 
            resetCommonCaches();
        }

    }


}
```

## 3 内置后置处理器

```java

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {
    
    @Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // 注册 BeanDefinition
        int registryId = System.identityHashCode(registry);
        if (this.registriesPostProcessed.contains(registryId)) {
            throw new IllegalStateException(
                    "postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
        }
        if (this.factoriesPostProcessed.contains(registryId)) {
            throw new IllegalStateException(
                    "postProcessBeanFactory already called on this post-processor against " + registry);
        }
        // 添加已经注册过的注册器 Id
        this.registriesPostProcessed.add(registryId);
        processConfigBeanDefinitions(registry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        
        // 修改 BeanDefinition
        int factoryId = System.identityHashCode(beanFactory);
        if (this.factoriesPostProcessed.contains(factoryId)) {
            throw new IllegalStateException(
                    "postProcessBeanFactory already called on this post-processor against " + beanFactory);
        }
        this.factoriesPostProcessed.add(factoryId);
        if (!this.registriesPostProcessed.contains(factoryId)) {
            // 对应的注册器 Id 没有注册过, 先进行 BeanDefintion 的注册
            processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
        }

        enhanceConfigurationClasses(beanFactory);
        
        // 添加了一个 BeanPostProcessor
        beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
    }
    
    public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
        
        // 需要处理的配置类候选者
        List<BeanDefinitionHolder> configCandidates = new ArrayList<>();

        // 已经注册的 BeanDefinition 的名称
		String[] candidateNames = registry.getBeanDefinitionNames();

        for (String beanName : candidateNames) {

            BeanDefinition beanDef = registry.getBeanDefinition(beanName);

            // 对应的 BeanDefinition 有个属性 org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass， 同时属性值为 full 或 lite
            if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) || ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
                // log 打印一下日志, 不做任何处理
            } else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {

                // ConfigurationClassUtils.checkConfigurationClassCandidate 作用
                // 检查给定的 BeanDefinition 是否是配置类的候选者, 是的话，会打上标记也就是给 BeanDefiniton 新增属性 org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass,
                // 取值为 full 和 lite 

                // 写上了 org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass 属性表示这个 BeanDefinition 已经解析过了, 可以不解析了
                // full 模式, @Configuration 注解的类为 full
                // lite 模式, 不是 @Configuration 注解的类, 也不是接口类的情况下
                // 1. 注解了 @Import @ImportResource @Component @ComponentScan 一个或者多个
                // 2. 类中有 @Bean 注解的方法
                // 3. 在 Spring 5.2 之后, @Configuration 里面多了一个 proxyBeanMethods 属性, 设置为 false, 这个 @Configuration 类也会当做 lite 模式
                // 这三种情况为 lite 模式

                // 区分 full 和 lite 2 种模式的原因
                // 正常情况下, @Configuration 类中, 会有 @Bean 注解的方法, 里面的通常都是 new 一个对象, 同时支持 @Bean 注解的方法调用另一个 @Bean 注解的方法
                // 在 Spring IOC 容器中, @Bean 注解的方法，正常情况下，无论调用多少次应该返回的都是同一个对象, 这个对象应该是存在 Spring IOC 容器中的
                // 那么正常的配置类 @Bean 注解的方法, 是没法做到这种效果的, 所以需要对这个 @Bean 注解的方法做一下增强, 逻辑如下:
                // @Bean 注解的方法名作为 beanName, 调用这个方法时, 先通过 getBean(beanName) 到容器中查找, 找到返回这个 bean, 找不到, 再走下面的 new 逻辑，然后将这个 new 出来的对象放入容器, 返回这个 new 的对象
                
                // 在这个前提下,
                // full 模式的配置类会被 CGLIB 增强, 放入到 IOC 容器中的是代理类, 而 lite 模式不会被增强, 放入到 IOC 容器中的就是配置类本身
                // full 模式下的 @Bean 方法不能是 final 和 private 的, 因为需要进行方法代理, 而 lite 模式没有限制
                // full 模式下的 @Bean 方法可以调用其他的 @Bean 方法, 因为进行了代理，每次都是新对象，而 lite 模式下，@Bean 方法不建议没法调用其他 @Bean 方法，因为每次都是 new, 新对象, 通过参数传参来进行设置

                // 原因:
                // 因为 full 模式, 运行时会给该类生成一个 cglib 子类放进容器，有一定的性能, 时间开销 (一旦配置类多了, 会影响性能)
                // 而 @Bean 方法创建的都是新对象的问题，通过通过参数注入解决, 可以不生成代理类的，所以有了 lite 模式, 减少启动时的时间消耗

                // 方法内部的逻辑如下:
                // 1. BeanDefinition 里面没有 className, 或者配置了工厂方法 factoryMethodName, 不是候选者, 返回结束
                // 2. BeanDefinition 对应的 Class 上注解了 @Configuration, 设置 org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass 属性, 值为 full
                // 3. BeanDefinition 对应的 Class 没有注解 @Configuration, 
                // 3.1 是接口, 不是候选者， 返回结束
                // 3.2 注解了 @Import @ImportResource @Component @ComponentScan 一个或者多个, 设置 org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass 属性, 值为 lite
                // 3.3 类中有 @Bean 注解的方法, 设置 org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass 属性, 值为 lite
                // 4. 如果类上注解了 @Order, 获取注解里面的值, 设置 org.springframework.context.annotation.ConfigurationClassPostProcessor.order 属性, 值为 Order 注解上面的值

                // 添加到候选者列表，后面进行解析处理
                configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
            }
        }

        if (configCandidates.isEmpty()) {
			return;
		}

        // 排序, 从小到大
        configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});

        SingletonBeanRegistry sbr = null;
		if (registry instanceof SingletonBeanRegistry) {
            sbr = (SingletonBeanRegistry) registry;
            // localBeanNameGeneratorSet 默认为 false, 
			if (!this.localBeanNameGeneratorSet) {
			    // CONFIGURATION_BEAN_NAME_GENERATOR = org.springframework.context.annotation.internalConfigurationBeanNameGenerator
                // 默认获取到的 generator 为空
                BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
                if (generator != null) {
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
            }
        }

		// 正常情况下 environment 不为 null
        if (this.environment == null) {
            this.environment = new StandardEnvironment();
        }

        // 创建用于解析 @Configuration 注解的解析器
        ConfigurationClassParser parser = new ConfigurationClassParser(this.metadataReaderFactory, this.problemReporter, this.environment,
                this.resourceLoader, this.componentScanBeanNameGenerator, registry);

        // 需要处理的配置类候选者
        Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
        
        // 存储已经解析完成的配置 Class
        Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());

        do {
            // 解析验证

            // 1. 解析的类配置了 @Conditional, 有的话，条件是否符合，不符合的话会跳过解析
            // 2. 如果注解了 @Component, 先解析内部成员
            // 3. 解析 @PropertySource 注解, 如果有的
            // 4. 解析 @ComponentScan @ComponentScans 注解, 同样会判断是否注解了 @Conditional 决定是否解析, 得到需要解析的 Class 列表
            // 4.1 创建出 ClassPathBeanDefinitionScanner 实例, 根据 @ComponentScan 中的配置, 定制化 ClassPathBeanDefinitionScanner 实例，也就是 set 值, 如扫描的包含器, 过滤器等
            // 4.2 获取到所有的配置的包路径
            // 4.3 调用 ClassPathBeanDefinitionScanner.doScan 扫描所有的包路径 (每一个包路径内部会被替换为 class*/包路径(. 用 / 替换)/**/*.class, 里面包含的所有的 Class 都会转为一个 Resource 对象)
            // 4.4 通过 ComponentScan 里面的过滤器进行过滤处理，同样会判断是否有 @Conditional 注解, 找到对应的需要解析的类
            // 5. @Lazy @Primary @DependsOn @Role @Description 获取对应的配置设置到 BeanDefinition 中
            // 6. 向容器中注册这个 BeanDefinition
            // 7. 解析 @Import 注解
            // 8. 解析 @ImportResource 注解
            // 9. 解析 @Bean 注解的方法
            // 10. 如果实现了其他接口, 接口中也有 @Bean 方法, 也一起解析
            // 11. 如果有父类, 也需要进行父类的解析
            parser.parse(candidates);

            // 校验所有解析出来的类
            // @Configuration 注解的类, 不能是 final 的话, 进行校验, @Bean 注解的方法需要是可重载的，即不能是 static, final, private 的
            parser.validate();

            // 从配置类的候选者获取解析后的配置类
            Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
            
            // 去除已经解析过的
            configClasses.removeAll(alreadyParsed);

            if (this.reader == null) {
                // 从 ConfigurationClass 中读取 BeanDefinition 的读取器, 
                // ConfigurationClassBeanDefinitionReader 主要是针对配置类内部的 Bean 声明, 如 @Bean 注解的方法解析, @ImportSource, @Import 内容 的解析 
                this.reader = new ConfigurationClassBeanDefinitionReader(registry, this.sourceExtractor, this.resourceLoader, 
                        this.environment, this.importBeanNameGenerator, parser.getImportRegistry());
            }

            // 读取 BeanDefinition
            this.reader.loadBeanDefinitions(configClasses);
            // 把当前已经读取过的配置类 添加到 已解析完成的配置列表
            alreadyParsed.addAll(configClasses);
            
            // 清
            candidates.clear();
            
            // 当前注册的 BeanDefinition 比解析前的多，也就是这次配置候选者中解析出了 BeanDefinition
            if (registry.getBeanDefinitionCount() > candidateNames.length) {
                
                // 获取到当前全部已经注册的 BeanDefinition 的名称
                String[] newCandidateNames = registry.getBeanDefinitionNames();
                
                // 本次解析之前已经解析的 BeanDefinition 名称
                Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
                
                // 获取本次解析的配置类候选者的名称
                Set<String> alreadyParsedClasses = new HashSet<>();
                for (ConfigurationClass configurationClass : alreadyParsed) {
                    alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
                }

                // 遍历最新的 BeanDefinition 名称
                for (String candidateName : newCandidateNames) {
                    // 找出这次解析中新增的 BeanDefinition
                    if (!oldCandidateNames.contains(candidateName)) {
                        // 获取这个 BeanDefinition 
                        BeanDefinition bd = registry.getBeanDefinition(candidateName);
                        
                        // 再次判断这个 BeanDefinition 是否为配置类并且其不是这次解析过的配置候选者
                        // 都是的话, 将其添加到一个空的配置候选者列表, 再次解析这个配置候选者列表, 直到最终的配置候选者列表为空
                        // 其本质就是解析配置类里面的注解
                        if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
                                !alreadyParsedClasses.contains(bd.getBeanClassName())) {
                            candidates.add(new BeanDefinitionHolder(bd, candidateName));
                        }
                    }
                }
                candidateNames = newCandidateNames;
            }
            
        } while (!candidates.isEmpty());

        // IMPORT_REGISTRY_BEAN_NAME = org.springframework.context.annotation.ConfigurationClassPostProcessor.importRegistry
        if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
            // 注册一个单例 bean
            sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
        }

        // 解析中的一些缓存, 进行清空
        if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
            ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
        }
    }

    public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
        Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            
            // 这个 BeanDefinition 是 Full 模式
            if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
                if (!(beanDef instanceof AbstractBeanDefinition)) {
                    throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
                            beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
                } else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
                    // 打印日志
                }

                configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
            }
        }
        
        // 配置 Class 增强器
        ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();

        for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
            AbstractBeanDefinition beanDef = entry.getValue();
            
            // 设置了一个属性, 表示这个 BeanDefintion 被增强过
            // PRESERVE_TARGET_CLASS_ATTRIBUTE = org.springframework.aop.framework.autoproxy.AutoProxyUtils.preserveTargetClass
            beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);

            try {
                Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
                if (configClass != null) {
                    Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
                    if (configClass != enhancedClass) {
                        // 打印日志

                        // 设置 BeanDefinition 的 Class 为 cglib 增强后的 Class
                        beanDef.setBeanClass(enhancedClass);
                    }
                }
            } catch (Throwable ex) {
                throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
            }
        }
    }
}
```

EventListenerMethodProcessor


## 4. DefaultListableBeanFactory 的 preInstantiateSingletons 方法 

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

    @Override
    public void preInstantiateSingletons() throws BeansException {

        List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
        
        for (String beanName : beanNames) {

            // 先从 AbstractBeanFactory 的 Map<String, RootBeanDefinition> mergedBeanDefinitions 中通过 beanName 获取, 获取到就直接返回, 已经缓存过了
            // 获取不到, 走下面的逻辑
            
            // 1. DefaultListableBeanFactory 的 Map<String, BeanDefinition> beanDefinitionMap 获取这个 beanName 一开始的 BeanDefinition (RootBeanDefinition 类型)
            
            // 2. 从获取到的 BeanDefinition 中获取其父级 BeanDefinition 的 beanName
            
            // 2.1 获取到的为空, 没有父 BeanDefinition
            // 2.1.1 获取到的 BeanDefinition 为 RootBeanDefinition, 调用其 cloneBeanDefinition 方法, 得到一个全新的 RootBeanDefinition
            // 2.1.2 获取到的 BeanDefinition 不为 RootBeanDefinition 类型, new 一个 RootBeanDefinition, 将获取到的 BeanDefinition 的属性赋给新的 RootBeanDefinition
            
            // 2.2 获取到的不为空, 也就是有父 BeanDefinition, 如果父级的 BeanDefinition 的 beanName 和当前处理的 beanName 一样
            // 2.2.1 一样, 通过递归获取到父级 beanName 对应的 BeanDefinition
            // 2.2.2 不一样, 获取父级 BeanFactory, 父级为 ConfigurableBeanFactory 类型时, 从父级的 BeanFactory 中获取父级 beanName, 对应的 BeanDefinition
            // 2.2.3 将获取到的新的 BeanDefinition 当做参数, 传给 new RootBeanDefinition(), 深拷贝得到一个全新的 RootBeanDefinition
            // 2.2.4 将一开始的 BeanDefinition 里面配置的值覆盖新的 BeanDefinition 里面的值,         
            // 2.2.5 需要注意的是, 2.2.2 获取到的父级不为 ConfigurableBeanFactory 会直接抛异常结束
            
            // 2.3 设置获取到的新的 RootBeanDefinition 的 bean 范围为单例 singleton
            // 2.4 如果 beanName 对应的一开始的 BeanDefinition 不是单例的, 设置新的 BeanDefinition 的 bean 范围和一开始获取到的 BeanDefinition 一样
            // 2.5 把 beanName 和最新的 RootBeanDefinition 存到 AbstractBeanFactory 的 Map<String, RootBeanDefinition> mergedBeanDefinitions 中
            // 2.6 返回最新的 RootBeanDefinition
            
            // 简单的来说就是, 通过配置文件, 注解生产的 BeanDefinition, 通过深拷贝, 得到一个全新的 RootBeanDefinition
            // 因为 BeanDefinition 允许其有一个父级的 BeanDefinition, 这个全新的 RootBeanDefinition 的属性需要是 2 个 BeanDefinition 的组合, 重复时以子 BeanDefinition 为主 
            // 最终将全新的 RootBeanDefinition 存放一份到 AbstractBeanFactory 的 Map<String, RootBeanDefinition> mergedBeanDefinitions 中
            RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);

            // 1. 对应的 RootBeanDefinition 不是抽象类
            // 2. 对应的 RootBeanDefinition 配置为单例
            // 3. 对应的 RootBeanDefinition 不是懒加载的
            if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
                
                // beanName 对应的 bean 不是工厂 Bean
                // 判断的逻辑如下
                // 1. 先通过 beanName 从 Spring IOC 容器中获取对应的 bean
                // 2. 获取到了, 判断这个 bean 是否为 FactoryBean 的子类 即可
                // 3. 获取不到, 尝试从其 BeanDefinition 中进行判断
                // 3.1 当前的 BeanFactory 中获取不到这个 beanName 的原始 BeanDefinition, 那么这个 BeanFactory 有父级 BeanFactory, 尝试通过父级去判断, 判断逻辑和下一步一样
                // 3.2 判断这个 BeanDefinition 的目标 class 是否实现了 FactoryBean 即可
                
                if (isFactoryBean(beanName)) {
                    
                    // beanName 前面加个 &, 获取这个新 BeanName 对应的 bean
                    // FactoryBean 方式创建 bean 时, 会创建出 2 个 bean
                    // 第一个为 FactoryBean 本身, beanName 为对应的 beanName 前面加一个 &
                    // 第二个为 FactoryBean 内部的 getObject 方法创建出来的 bean, beanName 就是原始的 beanName 
                    
                    // 获取 FactoryBean 自身的 bean
                    Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);

                    if (bean instanceof FactoryBean) {
                        FactoryBean<?> factory = (FactoryBean<?>) bean;
            
                        boolean isEagerInit;
                        // 判断这个 FactoryBean 是否需要提前实例化            
                        if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                            isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit, getAccessControlContext());
                        } else {
                            isEagerInit = (factory instanceof SmartFactoryBean &&((SmartFactoryBean<?>) factory).isEagerInit());
                        }

                        // 提前实例化
                        if (isEagerInit) {
                            getBean(beanName);
                        }
                    }
                }else {
                    // 获取 bean
                    getBean(beanName);
                }
            }
        }

        for (String beanName : beanNames) {
            Object singletonInstance = getSingleton(beanName);
            
            // 从所有的实例 bean 中获取实现了 SmartInitializingSingleton 接口的, 调用他们的 afterSingletonsInstantiated 方法
            if (singletonInstance instanceof SmartInitializingSingleton) {
                SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                        smartSingleton.afterSingletonsInstantiated();
                        return null;
                    }, getAccessControlContext());
                }
                else {
                    smartSingleton.afterSingletonsInstantiated();
                }
            }
        }
    }

    @Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

    /**
     *
     * @param name: 需要获取的 bean 的名称, 也可以是 bean 的别名
     * @param requiredType: 需要的类型, 这里为空
     * @param args： 获取 bean 需要的参数, 这里为空
     * @param typeCheckOnly: 类型检查, 这里为 false
     *
     */
    protected <T> T doGetBean(String name, Class<T> requiredType, Object[] args, boolean typeCheckOnly) throws BeansException {

        // 获取这个 bean 对应的真正的 beanName, 主要针对别名处理
        String beanName = transformedBeanName(name);
        Object bean;

        // 尝试从三级缓存中获取这个 beanName 对应的 bean
        Object sharedInstance = getSingleton(beanName);

        // 获取到的了需要的 bean 实例, 同时需要的参数为空
        if (sharedInstance != null && args == null) {

            // 省略打印日志

            // 针对 FactoryBean 的特殊处理
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);

        } else {
            
            // 判断 ThreadLocal<Object> prototypesCurrentlyInCreation 的值是否为当前的 beanName, 也就是当前的线程是否在创建的这个 bean
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

            // 获取其父级 BeanFactory
            BeanFactory parentBeanFactory = getParentBeanFactory();

            // 有父级的 BeanFactory, 同时当前的原始 beanDefinition Map 中没有这个 beanName 对应的 BeanDefintion
            // 尝试从其父级的 BeanFactory 获取这个 bean
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {

                // 获取原来的名字
                // 通过入参的 name 获取其真正的 beanName,
                // 如果 name 是 & 开头的, 那么获取到的 beanName 前面也需要加上一个 &
                // 返回这个 beanName 
                String nameToLookup = originalBeanName(name);

				if (parentBeanFactory instanceof AbstractBeanFactory) {
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(nameToLookup, requiredType, args, typeCheckOnly);
				} else if (args != null) {
                    return (T) parentBeanFactory.getBean(nameToLookup, args);
                } else if (requiredType != null) {
                    return parentBeanFactory.getBean(nameToLookup, requiredType);
                } else {
                    return (T) parentBeanFactory.getBean(nameToLookup);
                }
            }

            // 不需要类型检查
            if (!typeCheckOnly) {
                
                // 如果 AbstractBeanFactory Set<String> alreadyCreated 已经创建的 beanName 集合中, 如果包含这个 bean 的话, 就结束
                // 不包含
                // 对 AbstractBeanFactory Map<String, RootBeanDefinition> mergedBeanDefinitions 加同步锁，
                // 在检查一次 AbstractBeanFactory Set<String> alreadyCreated 中是否包含这个 beanName, 如果包含的话, 结束
                // 不包含
                // 从 AbstractBeanFactory Map<String, RootBeanDefinition> mergedBeanDefinitions 中移除这个 beanName 对应的  RootBeanDefinition
                // 从 DefaultListableBeanFactory 的 Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders 中移除这个 beanName 对应的 BeanDefinitionHolder
                // 向 AbstractBeanFactory Set<String> alreadyCreated 中添加这个 beanName, 表示这个 beanName 已经创建过了

                // 将这个 beanName 添加到已经创建的 beanName 集合
                // 同时将这个 beanName 对应的 最终的 BeanDefinition 移除, 从配置中解析出来的 BeanDefinition 还保存着
                // 然后下一步操作会重新获取一次最新的最终 BeanDefinition, 确保用于创建的是最新的
				markBeanAsCreated(beanName);
			}


            try {

                // 重新获取一次最终的 BeanDefinition, 这里会重新添加到缓存中  AbstractBeanFactory 的 Map<String, RootBeanDefinition> mergedBeanDefinitions
                RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

                // 如果对于的最终的 BeanDefintion 是抽象类, 抛出异常
				checkMergedBeanDefinition(mbd, beanName, args);

                // 获取其声明的依赖列表
				String[] dependsOn = mbd.getDependsOn();

				if (dependsOn != null) {
                    for (String dep : dependsOn) {

                        // 循环依赖检查
                        // 1. 对  DefaultSingletonBeanRegistry 的 Map<String, Set<String>> dependentBeanMap 加同步锁
                        // 2. 获取对应的 beanName 的真正的 name, 
                        // 3. 从 Map<String, Set<String>> dependentBeanMap 中真正的 beanName 的集合, 也就入参的 beanName 被哪些 bean 依赖着, 列表为空, 直接返回 fasle
                        // 4. 如果被哪些 bean 依赖着的列表中包含入参的 dep 这个 bean, (A 被 B 依赖着, 创建 B, 那么就需要创建 A, 而现在是明确了 A 依赖于 B), 直接返回 false
                        // 5. 再次检查获取到的集合其他是否有链式的循环依赖， A->B->C->A

                        // 确定循环依赖了, 抛出异常
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}

                        // 1. DefaultSingletonBeanRegistry Map<String, Set<String>> dependentBeanMap  beanName 被哪些 bean 依赖
                        // 2. Map<String, Set<String>> dependenciesForBeanMap  beanName 依赖于哪些 bean

                        // 3. 向 dependentBeanMap 中 dep 的集合添加 beanName, 表示当前需要创建的 dep 这个 bean 被 beanName 依赖着
                        // 4. 向 dependenciesForBeanMap 中 beanName 的集合添加 dep, 表示创建 beanName 这个 bean, 需要依赖 dep 
						registerDependentBean(dep, beanName);
						try {
                            // 获取对应的 bean
							getBean(dep);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName, "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
                }

                // 单例处理
                if (mbd.isSingleton()) {
                    // getSingleton 方法 2 个参数， beanName，和一个 ObjectFactory 的函数接口
                    sharedInstance = getSingleton(beanName, () -> {
                        try {
                            return createBean(beanName, mbd, args);
                        } catch (BeansException ex) {
                            destroySingleton(beanName);
                            throw ex;
                        }
                    });

                    // 从 AbstractAutowireCapableBeanFactory 的 NamedThreadLocal<String> currentlyCreatedBean 中获取当前的线程真正创建的 beanName
                    // 获取到的 beanName 不为空，
                    // 1. 向 DefaultSingletonBeanRegistry Map<String, Set<String>> dependentBeanMap 中添加入参的 name， value 获取获取到的 beanName
                    // 2. 向 Map<String, Set<String>> dependenciesForBeanMap  中添加获取到的 beanName, value 为入参的 name
                    // 其实就是添加了一些依赖, 如果是正常没有依赖的类, 获取到的 beanName 将为空

                    // 3 当前的入参的 beanName 是否为工厂引用 (以 & 开头)
                    // 3.1 是的话, 当前的 bean 实例为 NullBean, 直接返回
                    // 3.2 是的话, 当前的 bean 不是 FactoryBean 的实例， 抛异常
                    // 3.3 不是的话, 继续下面的流程

                    // 4 当前的 bean 不是 FactoryBean 的实例, 或者 beanName 以 & 开头, 直接返回 bean 实例
                    // 正常的 bean 其实到了这一步就结束了, 下面的是 FactoryBean 的处理

                    // 5 入参的 BeanDefinition 为空 (正常情况下, 不为空的)
                    // 5.1 从 FactoryBeanRegistrySupport 的 Map<String, Object> factoryBeanObjectCache 中获取这个 beanName 对应的 Object 对象， 那个 Map 其实存的就是这个 FactoryBean 创建出来的 bean 的缓存
                    // 5.2 获取到了 Object, 就是最终的对象, 直接返回
                    
                    // 6 将入参的实例强转为 FactoryBean

                    // 7. 入参的 BeanDefinition 为空, 同时原始的 BeanDefiniton 缓存 DefaultListableBeanFactory Map<String, BeanDefinition> beanDefinitionMap 中包含这个 beanDefiniton, 
                    // 7.1 从中获取获取最终的 BeanDefinition

                    // 8. 入参的 BeanDefinition 为空, 获取到的最终的 BeanDefintion 也为空， 
                    // 8.1 则变量合成 synthetic 为 false 
                    // 8.2 变量合成 synthetic 等于入参的 BeanDefinition 为空, 获取到的最终的 BeanDefintion 的 synhetic 的属性值
                    // synthetic 一般都是指 AOP 的切面切点等

                    // 9. 强制后的 FactoryBean 配置了单例, 同时当前的一级缓存已经包含了入参的 beanName 的实例
                


                    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                } else if (mbd.isPrototype()) {
                    // Prototype, 每次请求都创建一个
                    Object prototypeInstance = null;
					try {

                        // 获取 AbstractBeanFactory 的 ThreadLocal<Object> prototypesCurrentlyInCreation
                        // 把当前的 beanName 添加到里面, 表示真正创建这个 bean
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					} finally {
                        // 把当前的 beanName 从 ThreadLocal<Object> prototypesCurrentlyInCreation 中移除
						afterPrototypeCreation(beanName);
					}
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
                } else {
                    // 其他的作用范围
                    String scopeName = mbd.getScope();
					if (!StringUtils.hasLength(scopeName)) {
						throw new IllegalStateException("No scope name defined for bean ´" + beanName + "'");
					}

                    // AbstractBeanFactory 的 Map<String, Scope> scopes 中存储了其他 bean 范围, 创建的 bean 会存放到对应的范围内部的类似于 Map 中
					Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}

                    try {
                        // scope.get 内部的逻辑和 getSingleton 类似
                        Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							} finally {
								afterPrototypeCreation(beanName);
							}
						});

						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);

                    } catch (IllegalStateException ex) {
                        throw new BeanCreationException(beanName, "Scope '" + scopeName + "' is not active for the current thread; consider " +
								"defining a scoped proxy for this bean if you intend to refer to it from a singleton", ex);
                    }
                }

            } catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}

        }

        if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return convertedBean;
			}
			catch (TypeMismatchException ex) {
				// 省略打印的日志
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
    }
}
```


## 问题

ApplicationContext 和 BeanFactory 有什么区别

ApplicationContext 实现了 BeanFactory, 所以 2 者都具备了生产 Bean 的功能
而 ApplicationContext 在获取 Bean 的基础上, 丰富了更多的功能, 比如包扫描，解析, 国际化, Spring 容器生命周期等

2 者都能作为 Bean 的容器，
但是 BeanFactory 只能手动的一个一个的注册 BeanDefinition
而 ApplicationContext 提供了批量的方式, 比如配置文件，指定配置类

## 参考

[Spring的@Configuration配置类-Full和Lite模式](https://www.cnblogs.com/Tony100/p/14423334.html)