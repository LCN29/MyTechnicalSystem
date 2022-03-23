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

            // BeanDefinitionRegistryPostProcessor 偏重于 BeanDefinition 的注册, 优先级比较高
            // BeanFactoryPostProcessor            偏重于 BeanDefinition 的修改
        
            // 基于注解的 Spring BeanDefinition 的扫描都是通过 ConfigurationClassPostProcessor 这个 BeanDefinitionRegistryPostProcessor 实现的
        
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
            parser.parse(candidates);
            parser.validate();

            // 从配置类的候选者获取解析后的配置类
            Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
            
            // 去除已经解析过的
            configClasses.removeAll(alreadyParsed);

            if (this.reader == null) {
                // 从 ConfigurationClass 中读取 BeanDefinition 的读取器 
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


## 问题

ApplicationContext 和 BeanFactory 有什么区别

ApplicationContext 实现了 BeanFactory, 所以 2 者都具备了生产 Bean 的功能
而 ApplicationContext 在获取 Bean 的基础上, 丰富了更多的功能, 比如包扫描，解析, 国际化, Spring 容器生命周期等

2 者都能作为 Bean 的容器，
但是 BeanFactory 只能手动的一个一个的注册 BeanDefinition
而 ApplicationContext 提供了批量的方式, 比如配置文件，指定配置类

## 参考

[Spring的@Configuration配置类-Full和Lite模式](https://www.cnblogs.com/Tony100/p/14423334.html)