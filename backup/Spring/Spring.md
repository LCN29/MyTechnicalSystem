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

        // 获取这个 bean 对应的真正的 beanName, 主要针对别名处理和 FactoryBean 的, 
        // FactoryBean 的实例会在 beanName 前面加一个 &, 表示 FactoryBean 实例, getObject() 返回的是真正的 beanName
        // 如果入参的 name 为 & + beanName, 那么返回值为 beanName
        String beanName = transformedBeanName(name);
        Object bean;

        // 尝试从三级缓存中获取这个 beanName 对应的 bean
        // 
        // 注意这里，虽然是真正的 beanName, 但是可能获取到的是 FactoryBean 的实例, 
        // 如果要创建的 bean 是 FactoryBean 实例, 在第一次创建成功时, 存放到容器里面的是 真正的 beanName 和 FactoryBean 的实例
        // 所以才有下面 getObjectForBeanInstance 下面的逻辑处理, 确保获取到的入参的 name 对应的真正的 bean
        Object sharedInstance = getSingleton(beanName);

        // 获取到的了需要的 bean 实例, 同时需要的参数为空
        if (sharedInstance != null && args == null) {

            // 省略打印日志

            // getObjectForBeanInstance 主要处理的是 sharedInstance 为 FactoryBean, name 不是 & 开头的真正的 beanName

            // 从 AbstractAutowireCapableBeanFactory 的 NamedThreadLocal<String> currentlyCreatedBean 中获取当前的线程真正创建的 beanName
            // 获取到的 beanName 不为空， 正常情况下为空
            // 1. 向 DefaultSingletonBeanRegistry Map<String, Set<String>> dependentBeanMap 中添加入参的 name， value 获取获取到的 beanName
            // 2. 向 Map<String, Set<String>> dependenciesForBeanMap  中添加获取到的 beanName, value 为入参的 name
            // 其实就是添加了一些依赖, 如果是正常没有依赖的类, 获取到的 beanName 将为空

            // 3 当前的入参的 name 是否为工厂引用 (以 & 开头), 也就是实际要获取的 bean 就是 FactoryBean, 校验功能
            // 3.1 是的话, 当前的 bean 实例为 NullBean (Spring 中为了兼容 null, 特别声明的类, 无任何意义) , 直接返回
            // 3.2 是的话, 当前的 bean 不是 FactoryBean 的实例， 抛异常 (以 & 开头, 但是不是 FactoryBean, 跑异常)
            // 3.3 不是的话, 校验成功, 继续走下面的

            // 4. 入参的 bean 实例不是 FactoryBean 的实例, 直接返回入参的 bean 实例
            // 5. 入参的 bean 为工厂引用 (以 & 开头), 是的话, 直接返回入参的 bean 实例

            // 到了下面的逻辑, 基本就是需要获取的 bean 为真正的 beanName, 而实例 sharedInstance 却是 FactoryBean 

            // 6. 入参的 BeanDefinition 是否为空 (正常情况下, 不为空的)
            // 6.1 是的话, 先尝试从 FactoryBeanRegistrySupport 的 Map<String, Object> factoryBeanObjectCache 中获取这个真正的 beanName 对应的 Object 对象 
            // (FactoryBean 的 getObject() 解析成功一次后, 会保存一份到这里), 获取到了, 也就是真正的 bean 已经解析过了, 返回这个 Object， 也就是解析过的 bean
           
            // 7. 入参的 BeanDefinition 为不为空, 获取 BeanDefintion 为空同时从缓存中获取不到真正的 beanName 对应的 bean
            // 8. 将入参的 beanInstance 强转为 FactoryBean
            // 9. 入参的 BeanDefinition 为空, 同时原始的 BeanDefiniton 缓存 DefaultListableBeanFactory Map<String, BeanDefinition> beanDefinitionMap 中包含这个 beanName 对应的 beanDefiniton
            // 获取这个最新的 BeanDefinition
            // 10. 判断是否为是否为合成的, BeanDefinition 为 null, false, BeanDefinition 不为 null, 则等于 BeanDefinition 配置的 Synthetic 属性, 默认为 false, 如果 false 表示后面获取的 bean 需要进行
            // BeanPostProcessor 的加工， true 则不需要， 一般情况下， AOP 的切面, 切点等的类, 这个才会被标记为合成的

            // 到了下面的逻辑, 就是从 FactoryBean 中获取对应的

            // 11. 强制的 FactoryBean 为单例同时一级缓存 DefaultSingletonBeanRegistry Map<String, Object> singletonObjects 中已经有 beanName 对应的实例 bean 

            // 11.1 是
            // 11.1.1 对一级缓存加同步锁
            // 11.1.2 再次从 FactoryBeanRegistrySupport 的 Map<String, Object> factoryBeanObjectCache 中获取这个 beanName 对应的 bean, 获取到不为 null, 也就是可能其他线程已经创建了, 直接返回这个 bean
            // 11.1.3 获取不到， 调用 FactoryBean 的 getObject() 得到一个 bean, 
            // 11.1.3.1 获取的 bean 不为空,  这个 bean 就是我们需要的最原始的 bean
            // 11.1.3.2 获取的 bean 为空, DefaultSingletonBeanRegistry Set<String> singletonsCurrentlyInCreation 中包含这个 beanName (当前的 beanName 正在创建中), 直接抛异常, 否则就默认为一个 NullBean 对象
            // 11.1.4 经过 11.1.3 步, 获取到了一个 bean 实例了
            // 11.1.5 再次从 FactoryBeanRegistrySupport 的 Map<String, Object> factoryBeanObjectCache 中获取这个 beanName 对应的 bean
            // 11.1.5.1 获取到了, 用获取到的 bean, 直接返回这个 bean, 因为获取到了，表示有另一个线程往里面加了一个 bean 了, 为了保证是同一个 bean, 直接返回这个 bean 即可
            // 11.1.5.2 获取不到, 
            // 11.1.5.2.1 合成属性为 true, 判断这个 beanName 在一级缓存中是否存在, 存在把 11.1.3 获取到的 bean 添加到 FactoryBeanRegistrySupport 的 Map<String, Object> factoryBeanObjectCache 中, 
            // 返回这个 bean
            // 11.1.5.2.1 合成属性为 false, 把这个 beanName 添加到 DefaultSingletonBeanRegistry Set<String> singletonsCurrentlyInCreation, 表示这个 bean 真正创建中
            // 11.1.5.2.1.1 调用所有的 BeanPostProcessor 的 postProcessAfterInitialization 方法, 直到遇到第一个返回 null, 则获取上一个的返回结果，或者全部执行完的最终结果 
            // 11.1.5.2.1.2 把这个 beanName 从 DefaultSingletonBeanRegistry Set<String> singletonsCurrentlyInCreation 中移除
            // 11.1.5.2.1.3 再次判断 DefaultSingletonBeanRegistry 的 Map<String, Object> singletonObjects 中包含这个 beanName 吗, 包含, 把最终的 bean 添加到 FactoryBeanRegistrySupport 的 Map<String, Object> 
            // factoryBeanObjectCache 中, 返回这个 bean

            // 11.2 否
            // 11.2.1 调用 FactoryBean 的 getObject() 得到一个 bean, 
            // 11.2.1.1 获取的 bean 不为空,  这个 bean 就是我们需要的最原始的 bean
            // 11.2.1.2 获取的 bean 为空, DefaultSingletonBeanRegistry Set<String> singletonsCurrentlyInCreation 中包含这个 beanName (当前的 beanName 正在创建中), 直接抛异常, 否则就默认为一个 NullBean 对象
            // 11.2.2 经过 11.2.1 得到了一个需要的 bean
            // 11.2.3 合成属性为 false, 调用所有的 BeanPostProcessor 的 postProcessAfterInitialization 方法, 直到遇到第一个返回 null, 则获取上一个的返回结果，或者全部执行完的最终结果 
            // 11.2.4 返回这个 bean

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

                // 如果对于的最终的 BeanDefinition 是抽象类, 抛出异常
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


    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {

        // 给一级缓存加锁
        synchronized (this.singletonObjects) {
        
            // 从一级缓存中获取对应的 bean
            Object singletonObject = this.singletonObjects.get(beanName);

            // 获取到了直接返回, 否则尝试进行创建
            if (singletonObject == null) {
                // 一般为 false, 销毁 bean 单例时, 会先将其置为 false
                if (this.singletonsCurrentlyInDestruction) {
                    throw new BeanCreationNotAllowedException(beanName, "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                                "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
                }

                // 在 bean 实例之前
                // 判断 Set<String> inCreationCheckExclusions 是否包含当前的 beanName,  不需要检查的 Set 列表不包含这个 beanName， 一般情况下, 这个 Set 列表为空
                // 并且 向正在创建的 beanName 列表 DefaultSingletonBeanRegistry Set<String> singletonsCurrentlyInCreation  添加这个 beanName 失败, 则会抛出异常
                beforeSingletonCreation(beanName);

                boolean newSingleton = false;

                // private Set<Exception> suppressedExceptions; 
                // 不影响流程的异常列表 == null, 有异常就结束
                boolean recordSuppressedExceptions = (this.suppressedExceptions == null);

                // 用于记录创建中的业务异常
                if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}

                try {
                
                    // 通注入的 lamdba 表达式 ObjectFactory 获取 bean 
                    singletonObject = singletonFactory.getObject();
                    newSingleton = true;

                } catch (IllegalStateException ex) {
                    singletonObject = this.singletonObjects.get(beanName);
                    if (singletonObject == null) {
                        throw ex;
                    }
                } catch (BeanCreationException ex) {

                    if (recordSuppressedExceptions) {
                        for (Exception suppressedException : this.suppressedExceptions) {
                            ex.addRelatedCause(suppressedException);
                        }
                    }
                    throw ex;
                } finally {

                    if (recordSuppressedExceptions) {
                        this.suppressedExceptions = null;
                    }
                    // 判断 Set<String> inCreationCheckExclusions 是否包含当前的 beanName,  不需要检查的 Set 列表不包含这个 beanName
                    // 并且 向正在创建的 beanName 列表 Set<String> singletonsCurrentlyInCreation  移除这个 beanName 失败, 则会抛出异常
                    afterSingletonCreation(beanName);
                }
            }

            // 添加到一级缓存, 同时清除二三级缓存, 同时把 beanName 添加到已经创建的 beanName 列表 DefaultSingletonBeanRegistry Set<String> registeredSingletons 
            if (newSingleton) {
				addSingleton(beanName, singletonObject);
			}

            // 返回需要的 bean
            return singletonObject;
        }
    }

    protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {

        // 获取最终的 beanDefinition
        RootBeanDefinition mbdToUse = mbd;
        // 获取 bean 的 class 类型
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);

        // 确保用于解析的 BeanDefinition 的 class 不为空
        // 解析出来的 class 不为空
        // BeanDefinition 的 Object beanClass 属性为不是 Class 类型
        // BeanDefinition 的 Object beanClass 属性不为空, 同时为字符串类型 
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            // 通过入参的 BeanDefiniton 拷贝一份到最终的 BeanDefinition 中, 同时设置最终的 BeanDefiniton 的 class 我解析出来的 class
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

        try {

            // 处理 BeanDefinition 中的 MethodOverrides 即处理  lookup-method 和 replaced-method 2 种情况
            // 如果 BeanDefinition 中的 MethodOverrides methodOverrides 不为空
            // 遍历 MethodOverrides methodOverrides 中所有的 Set<MethodOverride>
            // 获取对应的项, 得到对应项的方法名
            // 对应的 bean 中对应的方法名, 数量为 0 个, 抛异常
            // 对应的 bean 中对应的方法名, 数量我 1 个，设置对应项 MethodOverride 的 overloaded 为 false， 默认为 true, 表示不用在解析了
            // 避免在多个相同方法名时, 需要通过参数类型, 个数等进行匹配检查
			mbdToUse.prepareMethodOverrides();
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

         try {

            // 让 BeanPostProcessors 有机会返回一个代理而不是目标 bean 实例 

            // 最终的 BeanPostProcessor 中的 Boolean beforeInstantiationResolved 为 true 吗? (这个属性表示在这个 bean 实例化前是否允许 BeanPostProcessor 其作用, 尝试生成一个代理 bean)
            // 不是, 返回 null
            // 是
            // 1. BeanDefinition 中的合成属性 boolean synthetic 为 true, 直接返回 null
            // 2. 当前容器中有 InstantiationAwareBeanPostProcessor 的 BeanPostProcessor 吗， 没有直接返回 null
            // 3. 从 BeanDefinition 中获取需要产生的 bean Class 类型, 获取到的为空, 直接返回 null
            // 4. 调用所有 InstantiationAwareBeanPostProcessor 的 postProcessBeforeInstantiation 方法, 直到第一个返回值不为 null 或者全部执行为, 还是为 null
            // 5. 调用 InstantiationAwareBeanPostProcessor 得到的 bean 为 null, 直接返回 null
            // 6. 调用所有的 BeanPostProcessor 的 postProcessAfterInitialization 方法，直到第一个返回 null, 就返回上一个的值，或者执行到最后一个，还是不为 null, 返回这个最终的值
            // 7. 将获取的 bean, 返回给调用方
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);

            // 获取到的 bean 不为空, 直接返回这个 bean 就行了
            if (bean != null) {
                return bean;
            }

        } catch(Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName, "BeanPostProcessor before instantiation of bean failed", ex);
        }

        try {
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            return beanInstance;
        } catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
        }
    }

    protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {

        BeanWrapper instanceWrapper = null;

        // BeanDefinition 配置的是单例

        if (mbd.isSingleton()) {
            // 从 AbstractAutowireCapableBeanFactory ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache 中删除这个 beanName 对应的 bean, 同时获取删除的 BeanWrapper
            // factoryBeanInstanceCache 存储的是 未完成的 FactoryBean 和其对应的 BeanWrapper
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }

        // BeanWrapper 还是为空
        if (instanceWrapper == null) {
            // 根据最终的 BeanDefinition 和参数，创建出一个 BeanWrapper, 这时 Bean 实例 已经在方法内初始化好了

            // 内部的逻辑大体如下
            // 获取 bean 的 Class  类型不为空, Class 不是 public， BeanDefinition 的 boolean nonPublicAccessAllowed (是否允许通过反射返回非共有的构造函数和方法) 属性为 true
            // 直接抛异常


            // A 创建 Bean 实例方式一, 通过 BeanDefinition 配置的 Supplier
            // 获取 BeanDefinition 的 Supplier<?> instanceSupplier, 内置的实例提供器
            // 不为空, 调用这个实例提供器，尝试获取需要的 bean, 获取到的为空，封装为 NullBean, 将获取到的 Bean 包装为 BeanWrapper, 返回给方法调用方
            
            // B 有工厂方法, 通过工厂方法进行创建
            // 如果 BeanDefinition 中配置了工厂方法 String factoryMethodName 的话，通过工厂方法进行创建

            // C 已经将工厂方法或者构造函数缓存起来了, 通过缓存的工厂方法或者构造函数创建

            // BeanDefinition Executable resolvedConstructorOrFactoryMethod 是否为空 (已经解析过的构造函数或者工厂方法缓存)
            // 1. 不为空, 获取 BeanDefinition boolean constructorArgumentsResolved (构造函数是否已经解析过了)
            // 1.1 构造函数已经解析了, 通过 ConstructorResolver 的 autowireConstructor 进行创建对象，也就是通过缓存过的构造函数或工厂方法创建对象
            // 1.2 构造函数没有解析, 通过 AbstractAutowireCapableBeanFactory 的 InstantiationStrategy instantiationStrategy (默认为 cglib), 进行创建对象

            // 2. 为空, 继续走下面的逻辑

            // D 如果配置了 SmartInstantiationAwareBeanPostProcessor 获取到的构造函数, 或者配置了构造函数参数列表, 强制构造函数创建, 入参的参数列表不为空, 通过构造函数创建
            // 如果有 SmartInstantiationAwareBeanPostProcessor 的 BeanPostProcessor 的话, 遍历所有的 SmartInstantiationAwareBeanPostProcessor
            // 执行其 determineCandidateConstructors 方法, 获取到对应的构造函数， 一直遍历到第一个非空的, 或者遍历到最后一个还是为空的
            
            // 获取到的构造函数不为空 
            // BeanDefinition 配置的是构造函数注入
            // BeanDefinition 中的构造函数列表 ConstructorArgumentValues constructorArgumentValues 不为空
            // 入参的参数列表不为空
            // 上面 4 个条件满足一个就通过 ConstructorResolver 的 autowireConstructor 进行创建对象，也就是通过构造函数进行创建

            // E 通过系统判断的最优的构造函数,进行创建
            // 获取 BeanDefinition 中配置的最优的构造函数，(调用的方法内部直接返回 null 了), 进行构造

            // F 通过 AbstractAutowireCapableBeanFactory 的 InstantiationStrategy instantiationStrategy (默认为 cglib), 进行创建对象

            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }

        // 获取 Bean 包装类里面的 Bean
        Object bean = instanceWrapper.getWrappedInstance();
        // 获取 Bean 包装类的 Class 类型
		Class<?> beanType = instanceWrapper.getWrappedClass();

        // 获取到的 Class 不为 NullBean, 设置 BeanDefinition 的 Class<?> resolvedTargetType 目标 Class 类型为对应的 Class 类型
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

        // 对 BeanDefinition 的 Object postProcessingLock 加同步锁
        synchronized (mbd.postProcessingLock) {
            // BeanDefinition 的 boolean postProcessed 为 false (表示这个 BeanDefinition 是否已经被 MergedBeanDefinitionPostProcessor 修改过了)
			if (!mbd.postProcessed) {
				try {
                    // 如果有 MergedBeanDefinitionPostProcessor 的 BeanPostProcessor
                    // 遍历所有的 MergedBeanDefinitionPostProcessor, 执行其 postProcessMergedBeanDefinition 方法, 对当前的 BeanDefinition 进行修改
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
                // 修改为已经修饰过了
				mbd.postProcessed = true;
			}
		}

        // 是否允许提前暴露实例 bean, 
        // BeanDefinition 为单例配置 
        // AbstractAutowireCapableBeanFactory boolean allowCircularReferences 为 true (是否允许自动尝试解决循环依赖, 默认为 true)
        // DefaultSingletonBeanRegistry Set<String> singletonsCurrentlyInCreation 真正创建的 bean 列表包含这个 bean
        // 都满足就是可以提前暴露
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));

        if (earlySingletonExposure) {
            // 对一级缓存 DefaultSingletonBeanRegistry Map<String, Object> singletonObjects 上锁
            // 如果一级缓存 DefaultSingletonBeanRegistry Map<String, Object> singletonObjects 还是不包含这个 beanName
            // 1. 向三级缓存 DefaultSingletonBeanRegistry Map<String, ObjectFactory<?>> singletonFactories 添加这个 beanName 和可以提前获取这个 bean 的函数接口
            // 2. 从二级缓存 Map<String, Object> earlySingletonObjects 早期创建对象集合中移除这 beanName
            // 3. 向 Set<String> registeredSingletons 中添加这个 beanName, 已经注册的 beanName 列表
            
            
            // AbstractAutowireCapableBeanFactory getEarlyBeanReference() 获取早期的 bean, 也就是实例化, 但是没有初始化的
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

        // 获取需要暴露的对象 bean 
        Object exposedObject = bean;

        try {
            // 填充属性
			populateBean(beanName, mbd, instanceWrapper);
            // 初始化

            // 1. 填充各个 Aware 接口的值 BeanNameAware  BeanClassLoaderAware  BeanFactoryAware
        
            // 2. 依次调用各个 BeanPostProcessor 的 postProcessBeforeInitialization 方法 
            // 2.1  ApplicationContextAwareProcessor 这个 BeanProcessor 会填充其他的 Aware 
            // EnvironmentAware EmbeddedValueResolverAware ResourceLoaderAware ApplicationEventPublisherAware MessageSourceAware ApplicationContextAware
            
            // 3. 当前的 bean 是 InitializingBean 的实例, 同时 这个 bean 对应的 BeanDefinition 为空或者 BeanDefinition 自定义的 init-method 不包含 afterPropertiesSet
            // 3.1 则调用 这个 bean 的 afterPropertiesSet 方法

            // 4. BeanDefinition 不为 null, bean 的 class 不是 NULLBean
            // 4.1 BeanDefinition 的 初始方法名 init—method 有配置的话, 调用配置的 init-methods 方法

            // 5. BeanDefinition 为空 或者 BeanDefinition 不是 Synthetic 合成的
            // 5.1 遍历所有的 BeanPostProcessor, 调用他们的 postProcessAfterInitialization 方法
			exposedObject = initializeBean(beanName, exposedObject, mbd);

        }catch (Throwable ex) {
            if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
                throw (BeanCreationException) ex;
            } else {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
            }
        }

        // 允许提前暴露
        if (earlySingletonExposure) {

            // 从单例缓存中尝试获取这个 beanName 对象， false， 不从第三层缓存获取
            Object earlySingletonReference = getSingleton(beanName, false);

            
            // earlySingletonReference 不为 null，说明存在循环引用
            // 为什么呢？因为第一个处理的时候，会将引用放到 singletonFactories 缓存中，当循环依赖注入的时候 
            // (也就是在上面填充属性时, 去获取其他 bean 中, 存在某个 bean 依赖于当前正在创建的这个 bean)，
            // 会通过 singletonFactories 中拿到提前暴露的引用，然后放到第二级缓存 earlySingletonObjects中。
            // 所以，在这里拿到了 earlySingletonReference，表明存在循环引用。

            // 存在
            if (earlySingletonReference != null) {

                // 如果相等， 没有被修改过引用, 代理了之类的，将 earlySingletonReference 返回回去即可
                if (exposedObject == bean) {
                    // 同一个对象
                    exposedObject = earlySingletonReference;

                    // allowRawInjectionDespiteWrapping 默认为 false,  Map<String, Set<String>> dependentBeanMap 包含这个 beanName 的依赖， (这个 beanName 被哪些 bean 依赖了)
                } else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                    // 获取依赖这个 beanName 的所有 beanName 数组
                    String[] dependentBeans = getDependentBeans(beanName);
                    Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                    for (String dependentBean : dependentBeans) {
                        // 如果已经创建的 bean 中存在依赖这个 beanName
                        // 1. 从单例缓存中移除这个 beanName 的实例
                        if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                            actualDependentBeans.add(dependentBean);
                        }
                    }
                    // 抛异常
                    if (!actualDependentBeans.isEmpty()) {
                        throw new BeanCurrentlyInCreationException(beanName,
                            "Bean with name '" + beanName + "' has been injected into other beans [" +
                            StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                            "] in its raw version as part of a circular reference, but has eventually been " +
                            "wrapped. This means that said other beans do not use the final version of the " +
                            "bean. This is often the result of over-eager type matching - consider using " +
                            "'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");

                    }
                }
            }

        }

        try {

            // 为当前的 bean 添加注册逻辑
            
            // 1. BeanDefinition 是 prototype 范围, 不需要注册消耗逻辑
            // 2. bean 是 NullBean 实例, 不需要注册消耗逻辑
            
            // 3. 下面的情况, 如果符合一个就是需要注册消耗逻辑
            // 3.1 当前的 bean 是 DisposableBean 或 AutoCloseable 的实例
            // 3.2 当前的 bean 声明了销毁方法, 销毁方法名不是 （inferred)
            // 3.3 当前的 bean 声明了销毁方法, 销毁方法名为 （inferred), 同时类中有 close 或者 shutdown 名的方法
            // 3.4 容器中有 DestructionAwareBeanPostProcessor 的 BeanPostProcessor, 同时里面存在至少一个 他们的方法 requiresDestruction(当前的 bean) 返回值为 true, 也就是这个 BeanPostProcessor 适用这个 bean
            // 3 中的情况满足一个, 就会注册消耗逻辑

            // 4. 当前的 bean 为单例
            // 4.1 是
            // 4.1.1 向容器 DefaultSingletonBeanRegistry 的 Map<String, Object> disposableBeans 注册这个 beanName 对应的销毁类 DisposableBean (默认实现为 DisposableBeanAdapter 实现)
            // 4.1.2 DisposableBean 中有一个 destory 的方法, new DisposableBeanAdapter 会把消耗中需要的逻辑都整理好，当 bean 消耗时, 会调用到到这个 DisposableBean 的 destory() 方法
            // 4.1.3 destory 方法执行的顺序
            // 4.1.3.1 DestructionAwareBeanPostProcessor 实例的 postProcessBeforeDestruction 方法
            // 4.1.3.2 当前 bean 为 DisposableBean 的子类， 调用自身的 destory 方法
            // 4.1.3.3 调用自身配置的 init-destory 方法

            // 4.2 不是
            // 4.2.1 从 DefaultSingletonBeanRegistry 容器的 Map<String, Scope> scopes 中获取这个 bean 对应的 Scope, 向这个 Scope 注册这个bean 对应的 销毁类 DisposableBean (默认实现为 DisposableBeanAdapter 实现)
            
            registerDisposableBeanIfNecessary(beanName, bean, mbd);

        } catch (BeanDefinitionValidationException ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
        }
        return exposedObject;
    }
}
```


## 问题

### 1 ApplicationContext 和 BeanFactory 有什么区别

ApplicationContext 实现了 BeanFactory, 所以 2 者都具备了生产 Bean 的功能
而 ApplicationContext 在获取 Bean 的基础上, 丰富了更多的功能, 比如包扫描，解析, 国际化, Spring 容器生命周期等

2 者都能作为 Bean 的容器，
但是 BeanFactory 只能手动的一个一个的注册 BeanDefinition
而 ApplicationContext 提供了批量的方式, 比如配置文件，指定配置类

### 2 单例 bean 的属性循环依赖的解决关键: 三级缓存

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

    /**
     * 一级缓存: 用于存放完全初始化好的 bean
     * key: beanName value: bean
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    /**
     * 二级缓存: 存放原始的 bean 对象(尚未填充属性), 用于解决循环依赖, 存放三级缓存 ObjectFactory 的执行结果, 确保其执行一次, 对象是同一个
     * key: beanName  value: 未填充属性的 bean
     */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    /**
     * 三级缓存: 存放 bean 工厂对象，用于提前暴露 Bean, 用于解决循环依赖, 
     * key： beanName, value: 可以获取到当前 bean 对象的 ObjectFactory 函数
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

    /**
     * 正在创建的 beanName 集合
     */
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    /**
     * 已经注册的 beanName
     */
    private final Set<String> registeredSingletons = new LinkedHashSet<>(256);
}
```

涉及到 Bean 的实例化的 4 个关键方法

> 1. getSingleton
> 2. doCreateBean
> 3. populateBean
> 4. addSingleton

假设现在有

```java
public class A {
    private B b;
}

public class B {
    private A a;
}
```


创建 class A 的 实例
> 1. 通过 getSingleton(String beanName, boolean allowEarlyReference) 去各级缓存中查找 (allowEarlyReference 为 true), 获取不到
> 2. A 实例未创建, 调用 getSingleton(String beanName, ObjectFactory<?> singletonFactory) 创建 A 的实例, 此时 A 还未创建, 调用 ObjectFactory.getObject 方法可以获取实例, getObject 实际是调用 createBean 进行 bean 的创建
> 3. 在 createBean 中, 会先创建出一个刚刚实例化的 A Bean
> 4. 要创建的 bean A 为单例, 正在创建的 beanName 集合包含当前的 beanName, 向三级缓存中添加可以提前获取最终 bean 引用的 ObjectFactory
> 5. 经过三, 四步得到了实例 A, 但是这时候 A 的属性还未初始化, 调用 populateBean 进行属性的填充, 通过 getSingleton(String beanName, boolean allowEarlyReference) 获取 B 的实例, 在各个缓存中获取不到, 开始创建类 B 的实例
> 5. 进行二, 三, 四步, 这时得到了实例 B, 同样调用 populateBean 进行属性的填充
> 6. 通过 getSingleton 获取 A 的实例时, 这次在第三层缓存中获取到了能得到 A 的 ObjectFactory 函数, 调用其 getObject 得到了A 的实例, 这时候 A 还未初始化, 把获取到的 A 添加到二级缓存, 从三级缓存中移除 A 
> 7. 调用 addSingleton 把实例 B 放到一级缓存, 从二三级缓存中删除 B
> 8. 这里又回到实例 A 的 populateBean 方法, 这时候获取到 B 的实例了, A 初始化完成, 
> 9. 这次从 一, 二 级缓存中获取 A 的 beanName, 获取到了, 和创建出来的 bean 不是同一个, 最终的 bean 为获取到的 (可能被代理了, 使用代理的)
> 10. 将最新的 A 添加到一级缓存中, 从二三级缓存中删除


```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {

    // 一级缓存获取
    Object singletonObject = this.singletonObjects.get(beanName);

    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {

        // 二级缓存获取
        singletonObject = this.earlySingletonObjects.get(beanName);

        if (singletonObject == null && allowEarlyReference) {

            // 对一级缓存加锁
            synchronized (this.singletonObjects) {
                // 再次检查

                // 从一级缓存获取
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    
                    // 从二级缓存获取
                    singletonObject = this.earlySingletonObjects.get(beanName);

                    if (singletonObject == null) {
                        // 从三级缓存获取
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            // 调用 ObjectFactory 获取对象
                            singletonObject = singletonFactory.getObject();
                            // 添加到二级缓存
							this.earlySingletonObjects.put(beanName, singletonObject);
							// 移除三级缓存
                            this.singletonFactories.remove(beanName);
                        }

                    }
                }

            }
        }

    }
    return singletonObject;
}
```

### 4. 三级缓存的必要性

一般循环依赖的流程
> 创建 A, 将可以提前获取 A 的 ObjectFactory 放到三级缓存
> 注入属性, 发现需要属性 B, 创建 B
> 创建 B, 发现需要 A, 在三级缓存中获得到 A 的 ObjectFactory, 调用获取到未完整的 A, 将 A 放入二级缓存，删除三级缓存
> 将 A 属性设置到 B, 将 B 放入一级缓存, 删除二三级缓存
> 回到创建 A，从一级缓存获取到 B, 设置到自身属性
> 将 A 从二三级缓存中删除, 添加到一级缓存

一级缓存存放的是完整的 Bean, 可以提供出去使用的
二级缓存存放的是实例化, 但是未初始化的 Bean
三级缓存存放的是可以提前获取到实例化, 但未初始化的 Bean

三级缓存的讨论, 需要涉及到 AOP 的
> 如果创建的 Bean 是有代理的，那么注入的就应该是代理 Bean，而不是原始的 Bean
> Spring 一开始并不知道 Bean 是否会有循环依赖
> 通常情况下（没有循环依赖的情况下），Spring 都会在完成填充属性，并且执行完初始化方法之后再为其创建代理。
> 但是，如果出现了循环依赖的话，Spring 就不得不为其提前创建代理对象，否则注入的就是一个原始对象，而不是代理对象。

ObjectFactory.getObject 实际调用的是下面的方法

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                // 如果需要代理，这里会返回代理对象；否则返回原始对象
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
}
```

在 Spring 中 SmartInstantiationAwareBeanPostProcessor 只有 2 个实现类
> 1. InstantiationAwareBeanPostProcessorAdapter: 一个适配器, 实现了 SmartInstantiationAwareBeanPostProcessor 所有方法, 但是返回的都是默认值, 没有任何实现
> 2. AbstractAutoProxyCreator

后者会涉及到 AOP 功能

```java

public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        // 记录已被代理的对象, 放入 2 层缓存
        this.earlyProxyReferences.put(cacheKey, bean);
        return wrapIfNecessary(bean, beanName, cacheKey);
    }
}
```

基于这些前提, 进行分析

#### 4.1 放弃第三层缓存

将 addSingletonFactory() 方法进行改造

```java
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized (this.singletonObjects) {
        // 判断一级缓存中不存在此对象
        if (!this.singletonObjects.containsKey(beanName)) { 
            // 直接从工厂中获取 Bean
            object o = singletonFactory.getObject(); 
            // 添加至二级缓存中
            this.earlySingletonObjects.put(beanName, o); 
            // 已经创建的 beanName 集合
            this.registeredSingletons.add(beanName);
        }
    }
}
```

这样的话，每次实例化完 Bean 之后就直接去创建代理对象，并添加到二级缓存中, 功能也是正常的。

但是这样会导致实例的代理对象的创建时间提前:
在三级缓存下：一般都是 bean 创建完成, 然后 bean 对象初始化后, 最后才进行代理。   
而在二级缓存下, 变成 bean 创建完成, 进行代理, bean 初始化。

但是这样违背了 Spring 设计原则: 在 Bean 初始化完成之后才为其创建代理


### 2.2 放弃第二层缓存

在 getSingleton() 方法中从第一层缓存获取不到, 同时当前的 beanName 在创建中, 会从二级缓存中获取, 获取到了, 返回。 
我们可以知道在二级缓存是创建成功, 但是未初始化的对象。

那么把第二层缓存舍弃, 存在 2 种情况
> 1. 一级缓存依旧强调是完整的 Bean, 那么在循环依赖时, 需要的属性需要都从三级缓存中获取
> 2. 一级缓存不强调是存完整的 Bean, 从三级缓存中获取到的 Bean, 可以直接存放到一级缓存

情况一: 
假设当前有 3 个类, A 依赖于 B 和 C, B 依赖于 A 和 C, C 依赖于 A 和 B。在没有第二层缓存。
> 1. 创建 A 的时候, 将 A 对应的 ObjectFactory 放到第三层缓存, 填充属性, 发现需要 B
> 2. 创建 B 的时候, 将 B 对应的 ObjectFactory 放到第三层缓存, 填充属性, 从第三层缓存中获取到了 A, 填充 C 属性, 发现没有  C 属性
> 3. 创建 C 的时候, 从第三层缓存中获取到了 A 和 B, 但是到了这里 A 对应的 ObjectFactory 的 getObject 方法会执行了 2 次, 需要确保 2 次获取到的 A 是同一个对象

情况二:
在 getSingleton 获取的 bean, 可能是未初始化的。所以将未初始化的对象支接放入到第一次缓存, 是可行的。
个人认为之所以不怎么做，应该是为了保证一次缓存的是完整的对象, 完全可以使用的, 而未完全的对象用另一个地方进行存放。

未初始的 bean 是没法直接使用的 (存在 NPE 问题), 所以 Spring 需要保证在启动的过程中，所有中间产生的 未初始的 bean 最终都会变成初始化的 bean
如果 未初始的 bean 和已初始的 bean 都混在一级缓存中, 那么为了区分他们，势必会增加一些而外的标记和逻辑处理，这就会导致对象的创建过程变得复杂化了
将未初始的 bean与已初始的 bean 分开存放, 两级缓存各司其职, 能够简化对象的创建过程, 更简单, 直观。


### 5. Spring Bean 生命周期

实例化 Instantiation
属性赋值 Populate
初始化 Initialization
销毁 Destruction


#### 5.1 实例化 Instantiation
获取到 beanName 对应的最完整的 BeanDefinition, (BeanDefinition 可以有父级, BeanFactory 也可以有父级, 所以 beanName 对应的从配置中解析处理的 BeanDefinition 不一定完整的, 需要合并父 BeanDefinition)

通过 BeanDefinition 获取到的 Bean 定义是抽象类, 不是单例, 设置了懒加载, 则跳过这个 bean 的初始化

通过最终的 BeanDefinition 获取到对应的 bean 是否为 FactoryBean

1. 不是

beanName 转换, (FactoryBean 的名称会在真正的 beanName 前面加 &, 别名转为真正的名称)
先从一级缓存中尝试获取这个 beanName 的实例, (当前的 beanName 不在创建中, 不会进入二三级缓存)
因为这里的 bean 不是 FactoryBean 所以获取到的就是需要的 bean, 不需要处理了

获取不到
当前的线程是否在创建这个 bean, 是抛出异常

有父 BeanFactory, 同时当前的 BeanFactory 从原始的 BeanDefinition 中获取不到这个 beanName 的 BeanDefinition, 
通过父级的 BeanFactory 进行创建

已经创建的 beanName 列表添加这个 beanName
获取 BeanDefinition 配置的 dependsOn 列表, 也就是显示的指明当前的 bean 需要依赖哪些 bean, 先创建他们

对一级缓存加锁, 确保对象的创建不受其他线程影响，
再次从一级缓存中获取这个 beanName 对应的 bean, 二次检查, 获取返回这个 bean

将当前的 beanName 添加到正在创建的 beanName 列表中

从 BeanDefinition 中获取到需要创建的 bean 的 Class 类型
对 BeanDefinition 中的方法进行检查, 在 xml 中支持配置 lookup-method 和 replaced-method 2 个特殊的方法, 检查配置了, 对应的 Class 是否有对应的方法, 没有抛异常, 有且一个, 设置 BeanDefinition 下次不用检查了

BeanDefinition 上有个 beforeInstantiationResolved (默认为 null, 如果为 true, 表示这个 bean 是特殊的 bean) 为 null 或者 true, 同时 Spring 容器中已经有了 InstantiationAwareBeanPostProcessor 的实例
> 遍历所有的 InstantiationAwareBeanPostProcessor 的 postProcessBeforeInstantiation 方法, 直到第一个返回值不为 null, 或者全部遍历完还是 null
> 得到的 bean 不为 null, 则继续遍历所有的 BeanPostProcessor 的 postProcessAfterInitialization 方法, 直到第一个返回 null, 则返回上一个的结果，或者全部遍历完，还是 null
> 得到的 bean 不为 null, 设置 BeanDefinition 的 beforeInstantiationResolved 为 true, 否则为 false
> 这样就得到了一个 bean, 如果这个 bean 不为 null, 那么就是我们需要的 bean 了
> 作用也就是让 BeanPostProcessor 有机会返回一个代理的 bean, 而不是真正的 bean 实例

上一步没有创建出代理 bean 的话, 进入真正的 bean 创建
通过工厂方法, cglib 代理，构造函数反射调用等方式, 获取到一个实例化的 bean 包装类

Spring 容器中有 MergedBeanDefinitionPostProcessor 的实例
遍历所有的 MergedBeanDefinitionPostProcessor 的 postProcessMergedBeanDefinition 方法(), 会尝试解析一些生命周期的信息, 如消耗方法, 初始方法等, 自动注解的属性
设置 BeanDefinition 的后置解析完成属性为 true

如果允许提前暴露引用的话, 向三级缓存中添加当前的 beanName 和可以提前获取当前 bean 的 ObjectFactory 函数

对 bean 填充属性了
属性中有个特殊点
Spring 容器中有 InstantiationAwareBeanPostProcessor 的实例, 遍历所有的 postProcessAfterInstantiation() 方法, 直到第一个返回值为 false, 结束, 
如果有一个返回了 false, 则不会进行后面的属性赋值,

当前的 bean 是BeanNameAware/BeanClassLoaderAware/BeanFactoryAware 强转为对应的类型, 然后调用他们的 set 方法设对应的值到 bean 中

遍历所有的 BeanPostProcessor 的 postProcessAfterInitialization 方法, 直到第一个返回 null, 则返回上一个的结果，或者全部遍历完，还是 null

强转为 InitializingBean, 调用 afterPropertiesSet() 方法， 如果实现了 InitializingBean 接口
调用自定义的 init-method 方法

遍历所有的 BeanPostProcessor 的 postProcessAfterInitialization 方法, 直到第一个返回 null, 则返回上一个的结果，或者全部遍历完，还是 null

从一二级缓存尝试获取当前 beanName 对应的 bean, 获取不到, 那么上面创建的 bean 就是最终的 bean 了
获取到了, 和当前的 bean 是一样的, 那么获取到的 bean 就是最终的 bean, 否则就按照异常处理了

将当前 Bean 的销毁逻辑封装为一个 DisposableBeanAdapter 对象, 放到 BeanFactory 中, 在 bean 销毁时, 执行这个 DispoableBeanAdapter 的 destory 方法, 逻辑如下
> 遍历所有的 DestructionAwareBeanPostProcessor 的 postProcessBeforeDestruction 方法
> 强转为 DisposableBean, 调用 destroy() 方法， 如果实现了 DisposableBean 接口
> 调用自定义的 destroy-method 方法

把当正在创建的 beanName 列表移除这个 beanName
把这个实例添加到一级缓存, 清除二三级缓存

还会执行一次 getObjectForBeanInstance() 因为不是 FactoryBean, 基本没逻辑

最后如果 bean 实现了 SmartInitializingSingleton, 调用其 afterSingletonsInstantiated() 方法


2. 是

FactoryBean 和上面的类似，只是入参的在后面的 getObjectForBeanInstance 会调用其 getObject 方法，创建出 bean, 同时将 bean 缓存到 Map<String, Object> factoryBeanObjectCache 中



### 6. BeanFactory 简介以及它和 FactoryBean 的区别

BeanFactory 是一个接口, 主要是规范了 IOC 容器的大部分的行为, 比如获取指定的 bean, IOC 容器中是否包含了某个 beanName 对应的 bean 等。
而 FactoryBean 同样也是一个接口, 为 IOC 容器中 Bean 的实现提供了更加灵活的方式, 通过实现其 getObject() 方法, 动态地通过代码的形式提供 bean 的创建方式。

BeanFactory 是一个 Factory，也就是 IOC 容器或对象工厂, 在 Spring 中, 所有的 Bean 都是由 BeanFactory (也就是 IOC 容器) 来进行管理的。  
而 FactoryBean 是一个比较特殊的 Bean, 是一个能生产或者修饰对象生成的工厂 Bean, 它的实现与设计模式中的工厂模式和修饰器模式类似。



## 参考

[Spring的@Configuration配置类-Full和Lite模式](https://www.cnblogs.com/Tony100/p/14423334.html)