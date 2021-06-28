
https://www.zhihu.com/question/21346206
https://www.cnblogs.com/wyq178/p/11415877.html

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

    AtomicBoolean active = new AtomicBoolean();

    AtomicBoolean closed = new AtomicBoolean();

    Object startupShutdownMonitor = new Object();

    Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

    private ResourcePatternResolver resourcePatternResolver = getResourcePatternResolver();

    // 延迟加载的, 需要时才加载
    private ConfigurableEnvironment environment = createEnvironment();

    protected ResourcePatternResolver getResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver(this);
    }

    protected ConfigurableEnvironment createEnvironment() {
        // 会将系统的属性和环境变量 加载到自身的  MutablePropertySources propertySources = new MutablePropertySources() 中
        return new StandardEnvironment();
    }
}
```

```java

public void setConfigLocations(@Nullable String... locations) {

    if (locations != null) {
        // 数组中存在为 null 的对象就抛出异常
        Assert.noNullElements(locations, "Config locations must not be null");

        this.configLocations = new String[locations.length];

        for (int i = 0; i < locations.length; i++) {

            //地址解析, 比如将输入的 "classpath:test.xml" 解析成真正的地址
            // 调用的 Environment 的 PropertySourcesPropertyResolver resolveRequiredPlaceholders 方法进行解析
            this.configLocations[i] = resolvePath(locations[i]).trim();
        }
    }
    else {
        this.configLocations = null;
    }
}

protected String resolvePath(String path) {
    // 调用到 AbstractApplicationContext 的 getEnvironment() 方法
    return getEnvironment().resolveRequiredPlaceholders(path);
}

```




```java

public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    @Override
	public void refresh() throws BeansException, IllegalStateException {

        synchronized (this.startupShutdownMonitor) {

            // refresh 之前的准备 
            // 0. 一些用户设置的必要属性校验

            // 1. Set<ApplicationListener<?>> earlyApplicationListeners 的初始
            // 2. 启动前期的事件容器初始 Set<ApplicationEvent> earlyApplicationEvents 在 multicaster 广播器初始之前起作用。
            prepareRefresh();

            // 创建 DefaultListableBeanFactory 
            // 从 Resource 中加载 BeanDefinition, 存入到 DefaultListableBeanFactory 的 Map<String, BeanDefinition> beanDefinitionMap, 存之前会判断 beanName
                // 当前是否已经有已经创建的 bean，

            // 把 beanName 放到 List<String> beanDefinitionNames 中

            // 把 别名放入 容器
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

            // 设置 BeanFactory 的容器的参数

            // Set<Class<?>> ignoredDependencyInterfaces 中追加的接口作用
            // 可以看一下这里 https://www.jianshu.com/p/3c7e0608ff1f

            // 大体的作用如下：
            // 有个接口 I, 这个接口有个要实现的方法 void setA(A); 
            // 然后有个类 C 实现了接口 I, 里面有个属性 A a, 这个属性在 xml 或注解设置了自动注入
            // 同时实现了 I 接口， 属性 A 还可以通过 set 方法进行配置
            // 这时候把接口放到了 ignoredDependencyInterfaces 中, 那么在属性自动配置的时候，就能进行跳过

            // Map<Class<?>, Object> resolvableDependencies

            // 大体的作用如下:
            // 在 Spring 注入中，默认是按类型注入的, 当出现同一个接口有多个实现时，那么就会出现注入失败
            // 可以通过这个指定某个类型，要注入的对象是什么
            prepareBeanFactory(beanFactory);

            try {
                // 空方法
                // 可以修改在标准初始化的 ApplicationContext 内部的 Bean Factory
                // 这个时候所有的 bean Definition 已经被加载了, 但是尚未有任何一个 bean 被实例化
                // 这里允许在具体的 ApplicationContext 实现类中注册特殊的 BeanPostProcessors 等。
                postProcessBeanFactory(beanFactory);

                // 调用作为 bean 注册在 Context 中的 BeanFactoryPostProcessor
                // 1. 先从容器中获取 BeanFactoryPostProcessor, 将其分为 2 类， 
                // 1.1 一类 registryProcessor, 实际为 BeanDefinitionRegistryPostProcessor 类型的
                // 1.2 二类 regularPostProcessors, 非 BeanDefinitionRegistryPostProcessor 类型的

                // 2. 从容器中的 beanDefinitionMap 中获取实现了 BeanDefinitionRegistryPostProcessor 和 PriorityOrdered 的类的 bean Name，找到并获取实例, 放入 currentRegistryProcessors
                // 3. 按照配置的 OrderComparator, 对 currentRegistryProcessors 进行排序
                // 4. 把 currentRegistryProcessors 全部放入到 registryProcessors 
                // 5. 依次调用 currentRegistryProcessors 中的 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 方法
                // 6. 清空 currentRegistryProcessors

                // 7. 从容器中的 beanDefinitionMap 中获取实现了 BeanDefinitionRegistryPostProcessor 和 Ordered 的类的 bean Name，找到并获取实例, 放入 currentRegistryProcessors，(还会过滤掉上面已经处理的 BeanDefinitionRegistryPostProcessor, 存在同时实现这 2 个接口的情况)
                // 8. 重复上面的 3, 4, 5, 6

                // 9. 从容器中的 beanDefinitionMap 获取到上面 2 步没有处理的 BeanDefinitionRegistryPostProcessor 
                // 10. 重复上面的 3, 4, 5, 6

                // 11. 调用 registryProcessors 的 postProcessBeanFactory 方法
                // 12. 调用 regularPostProcessors 的 postProcessBeanFactory 方法

                // 上面的步骤的大前提都是 beanFactory 为 BeanDefinitionRegistry 的子类, 否则会直接调用 从容器中获取 BeanFactoryPostProcessor 的 postProcessBeanFactory 的方法

                // 13. 从容器中的 beanDefinitionMap 中获取 BeanFactoryPostProcessor 的实现类的 bean Name 集合 postProcessorNames
                // 14. 声明一个 List<BeanFactoryPostProcessor> priorityOrderedPostProcessors 用于存储实现了 PriorityOrdered 的 BeanFactoryPostProcessor
                // 15. 声明了一个 List<String> orderedPostProcessorNames 存储实现了 Ordered 的 BeanFactoryPostProcessor,
                // 16. 声明了一个 List<String> nonOrderedPostProcessorNames 存储实现正常的 BeanFactoryPostProcessor,

                // 17. 循环获取到 postProcessorNames, 
                // 17.1 遍历的 beanName, 已经在上面的 BeanDefinitionRegistryPostProcessor 处理过的, 跳过
                // 17.2 遍历的 beanName 的实现类实现了 PriorityOrdered， 获取实例, 放入 priorityOrderedPostProcessors
                // 17.3 遍历的 beanName 的实现类实现了 Ordered, 将 beanName  放入 orderedPostProcessorNames
                // 17.4. 遍历的 beanName 放入到 nonOrderedPostProcessorNames

                // 18. 按照配置的 OrderComparator, 对 priorityOrderedPostProcessors 进行排序
                // 19. 依次调用 priorityOrderedPostProcessors 中的 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法

                // 20. 依次获取 orderedPostProcessorNames 中 beanName 的实例, 存入 List<BeanFactoryPostProcessor> orderedPostProcessors
                // 21. 按照配置的 OrderComparator, 对 orderedPostProcessors 进行排序
                // 22. 依次调用 orderedPostProcessors 中的 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法

                // 23. 依次获取 nonOrderedPostProcessorNames 中 beanName 的实例, 存入 List<BeanFactoryPostProcessor> nonOrderedPostProcessors
                // 22. 依次调用 nonOrderedPostProcessors 中的 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法

                // 24. 调用 beanFactory 的 clearMetadataCache，
                // 24.1 会清除 AbstractBeanFactory 中的 Map<String, RootBeanDefinition> mergedBeanDefinitions> 中在上面已经实例过的 BeanDefintion
                // 24.2 会清空 DefaultListableBeanFactory 中的 Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders
                // 24.3 清空 DefaultListableBeanFactory 中的 Map<Class<?>, String[]> allBeanNamesByType
                // 24.4 清空 DefaultListableBeanFactory 中的 Map<Class<?>, String[]> singletonBeanNamesByType

                invokeBeanFactoryPostProcessors(beanFactory);

                // 向 BeanFactory 注册 BeanPostProcessors, 创建 BeanPostProcessors 的实例


                // 1. 从容器中的 beanDefinitionMap 中获取实现了 BeanPostProcessor 的类的 beanName 集合 postProcessorNames
                // 2. 直接先注册一个 BeanPostProcessorChecker 的 BeanPostProcessor, 
                // BeanPostProcessorChecker 打印一条记录，内容为：在 BeanPostProcessors 实例化期间创建的 bean, 这些 bean 会不受 BeanPostProcessors 的影响

                // 3. 循环获取到 postProcessorNames
                // 3.1 声明一个 List<BeanPostProcessor> priorityOrderedPostProcessors 用于存储实现了 PriorityOrdered 的 BeanPostProcessor
                // 3.2 如果遍历的 BeanPostProcessor 在实现了 PriorityOrdered, 同时是 MergedBeanDefinitionPostProcessor 的子类, 再存储一份到  List<BeanPostProcessor> internalPostProcessors
                // 3.3 声明一个 List<String> orderedPostProcessorNames 用于存储实现了 Ordered 的 BeanPostProcessor
                // 3.4 声明一个 List<String> nonOrderedPostProcessorNames 用于存储剩余情况的 BeanPostProcessor
                
                // 4. 按照配置的 OrderComparator, 对 priorityOrderedPostProcessors 进行排序
                // 5. 将排序后的 priorityOrderedPostProcessors 依次放入到 BeanFactory 的 BeanPostProcessors 的 List<BeanPostProcessor> 容器中
                
                // 6. 遍历 List<String> orderedPostProcessorNames
                // 6.1 声明一个变量 List<BeanPostProcessor> orderedPostProcessors, 用于存储实例化的 BeanPostProcessor
                // 6.2 如果遍历的 orderedPostProcessorNames 的实现类如果是 MergedBeanDefinitionPostProcessor 的子类, 在存储一份到 List<BeanPostProcessor> internalPostProcessors
                
                // 7. 按照配置的 OrderComparator, 对 orderedPostProcessors 进行排序
                // 8. 将排序后的 orderedPostProcessors 依次放入到 BeanFactory 的 BeanPostProcessors 的 List<BeanPostProcessor> 容器中

				// 9. 遍历 List<String> nonOrderedPostProcessorNames
				// 9.1 声明一个变量 List<BeanPostProcessor> nonOrderedPostProcessors, 用于存储实例化的 BeanPostProcessor
				// 9.2 如果遍历的 nonOrderedPostProcessorNames 的实现类如果是 MergedBeanDefinitionPostProcessor 的子类, 在存储一份到 List<BeanPostProcessor> internalPostProcessors
				
                // 10. nonOrderedPostProcessors 依次放入到 BeanFactory 的 BeanPostProcessors 的容器中
                
                
                // 11. 重新注册 internal BeanPostProcessor
                // 12. 按照配置的 OrderComparator, 对 internalPostProcessors 进行排序
                // 13. 将排序后的 internalPostProcessors 依次放入到 BeanFactory 的 BeanPostProcessors 的 List<BeanPostProcessor> 容器中
                
                // 14. 添加到容器中的 BeanFactory 的 List<BeanPostProcessor>, 会去重，在添加的
                
                // 15. 在自主的向容器中添加一个 ApplicationListenerDetector 的 BeanPostProcessors
                // 16. 在 BeanFactory 创建的过程中，已自主的添加了 ApplicationContextAwareProcessor 和 ApplicationListenerDetector 2 个 BeanPostProcessor
                // 经过 15 的操作, ApplicationListenerDetector 将移到 List<BeanPostProcessor> 容器的最后一位
                
				registerBeanPostProcessors(beanFactory);

				// 1. 想容器中初始 messageSource 的单例 bean
                
                // 2. 判断 (已创建处理的单例是否包含这个 messageSource 的实例 ||  beanDefinitionMap 包含这个 messageSource) && (messageSource 不是引用 || messageSource 对应的 bean 不是 FactoryBean 的子类)
                // 2.1 如果为 true, 创建这个对应的 beanName 实例
                // 2.2 如果为 fasle, new DelegatingMessageSource 实例，尝试设置其 parentMessageSource, 通过 beanFacotory 的 parent
                // 2.3 把创建处理的 DelegatingMessageSource 实例放入到 BeanFactory 的单例容器中
                
                initMessageSource();

                // 初始化 ApplicationEvent 广播器
                
                // 1. 判断 (已创建处理的单例是否包含这个 applicationEventMulticaster 的实例 ||  beanDefinitionMap 包含这个 applicationEventMulticaster) && (applicationEventMulticaster 不是引用 || applicationEventMulticaster 对应的 bean 不是 FactoryBean 的子类)
                // 1.1 如果为 true, 创建这个实例
                // 1.2 如果为 false, new SimpleApplicationEventMulticaster(beanFactory) 实例
                // 1.3 把创建的 SimpleApplicationEventMulticaster 实例放入到 BeanFactory 的单例容器中
                
                initApplicationEventMulticaster();

				// 空方法
                // 子类可以重写这个方法, 进行处理
                onRefresh();

                // 注册事件监听器
                
                // 1. 从 BeanFactory 的 Set<ApplicationListener<?>> applicationListeners 获取已经注册在容器中的监听器
                // 2. 把第一步获取到的 ApplicationListener  添加到容器当前的 ApplicationEventMulticaster 的监听器集合中
                // 3. 从 BeanFactory 中获取所有 ApplicationListener 的实现类的 bean Name 集合 listenerBeanNames
                // 4. 遍历 listenerBeanNames 添加到 ApplicationListener 的 ListenerRetriever 的 Set<String> applicationListenerBeans 中
                
                // 5. 获取早期注册在 BeanFactory 的 Set<ApplicationEvent> earlyApplicationEvents 列表
                // 6. 置空 earlyApplicationEvents
                // 7. 遍历第 5 步获取到的 ApplicationEvent 列表, 调用 ApplicationEventMulticaster.multicastEvent 方法，广播给当前的 Listener
                registerListeners();

                // 实例化剩下的所有非 lazy-init 的单例 bean
                
                // 1. 判断 (已创建处理的单例是否包含这个 conversionService 的实例 ||  beanDefinitionMap 包含 conversionService 的 beanName)
                // 1.2 如果有, 判断这个 beanName 的 class 是否为 ConversionService 的实现类
                // 1.2.1 是的话, 创建这个 conversionService Bean, 设置 BeanFactory 的 ConversionService 为这个 bean
                
                // 2. BeanFactory 的 List<StringValueResolver> embeddedValueResolvers 是否为空, 
                // 2.1 如果为空的话, 填充一个默认的实现 (str)-> strVal -> getEnvironment().resolvePlaceholders(strVal)
                
                // 3. 从 BeanFactory 或获取实现 LoadTimeWeaverAware 的 beanName
                // 4. 遍历获取到的 beanName 获取实例
                
                // 5. 设置 BeanFactory 的 TempClassLoader 为 null
                // 6. 设置 BeanFactory 的 configurationFrozen 为 true, 标识后面的配置冻结了, 不允许修改
                // 7. 把当前所有的 beanDefinitionName 转为数组, 放到 BeanFactory 的 frozenBeanDefinitionNames
                // 8. 调用 BeanFactory 的 preInstantiateSingletons 实例化剩余的 bean
                finishBeanFactoryInitialization(beanFactory);

                // 缓存的清空

                // 1. clearResourceCaches DefaultResourceLoader 中的 Map<Class<?>, Map<Resource, ?>> resourceCaches 清空
                
                // 2. initLifecycleProcessor  初始生命周期处理器 
                // 2.1 容器中包含  lifecycleProcessor 的 bean
                // 2.1.1 尝试从 容器中获取 lifecycleProcessor 且类型为 LifecycleProcessor 的 bean, 设置当前 Application 的 LifecycleProcessor lifecycleProcessor 等于获取的 bean, 可能为空
                
                // 2.2 容器中不包含 lifecycleProcessor 的 bean
                // 2.2.1 设置当前 Application 的 LifecycleProcessor lifecycleProcessor 等于 DefaultLifecycleProcessor,
                // 2.2.2 想容器的 bean 缓存中添加这个 lifecycleProcessor， beanName 为 lifecycleProcessor

                // 3. 调用当前 Application 的 LifecycleProcessor lifecycleProcessor 的 onRefresh 方法
                // 3.1 如果  lifecycleProcessor 不为空, 执行 onRefresh 方法
                // 3.2 获取容器中注册的 Lifecycle 类型的 bean,  返回所以符合条件的 bean
                // 3.3 遍历所有的 bean
                // 3.4 入参为不允许自动执行 或者这个 bean 是 SmartLifecycle， 设置了允许自动执行, 继续，按照自身设置的执行优先度, 排序后，执行执行
                // 3.5 设置当前的 lifecycleProcessor 的 running 为 true

                // 4. 广播出一个  ContextRefreshedEvent 事件

                // 5. 调用 LiveBeansView 的 registerApplicationContext
                // 5.1 如果环境中配置了 spring.liveBeansView.mbeanDomain 属性值
                // 5.1.1 添加当前的 ConfigurableApplicationContext applicationContext 到 Set<ConfigurableApplicationContext> applicationContexts 中
                // 作为一个当前运行的快照, 后面可以通过这个输出为 json 字符串等
                
                finishRefresh();

            } catch (BeansException ex) {
                
                destroyBeans();
                cancelRefresh(ex);
                throw ex;

            } finally {

                // 1. ReflectionUtils.clearCache 清除 Map<Class<?>, Method[]> declaredMethodsCache 和 Map<Class<?>, Field[]> declaredFieldsCache 的缓存
                
                // 2. AnnotationUtils.clearCache 清除下面的缓存
                // 2.1 Map<AnnotationCacheKey, Annotation> findAnnotationCache
                // 2.2 Map<AnnotationCacheKey, Boolean> metaPresentCache
                // 2.3 Map<AnnotatedElement, Annotation[]> declaredAnnotationsCache
                // 2.4 Map<Class<? extends Annotation>, Boolean> synthesizableCache
                // 2.5 Map<Class<? extends Annotation>, Map<String, List<String>>> 
                // 2.6 Map<Class<? extends Annotation>, List<Method>> attributeMethodsCache
                // 2.7 Map<Method, AliasDescriptor> aliasDescriptorCache 

                // 3. ResolvableType.clearCache 清除 ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache 和 SerializableTypeWrapper 的 ConcurrentReferenceHashMap<Type, Type> cache

                // 4. CachedIntrospectionResults.clearClassLoader  getClassLoader()

                // 4.1 acceptedClassLoaders 从中清除当前的 ClassLoader 
                // 4.2 ConcurrentMap<Class<?>, CachedIntrospectionResults>  从中清除当前的 ClassLoader 
                // 4.3 ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache 从中清除当前的 ClassLoader 
                
                resetCommonCaches();
            }

        }

    }

}

```

PostProcessorRegistrationDelegate

```java
new DefaultListableBeanFactory(getInternalParentBeanFactory());


public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    // bean 实例化的策略
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    // 参数名获取这
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    // 尝试自动解决在 bean 之间的循环引用
    private boolean allowCircularReferences = true;

    private boolean allowRawInjectionDespiteWrapping = false;

    private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

    // 要忽略 依赖检查 和 自动装配 的依赖项接口
    // 在构造函数设置了 BeanNameAware BeanFactoryAware BeanClassLoaderAware
    private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

    private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

	private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

	private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache = new ConcurrentHashMap<>();

    // 是否允许重新注册一个相同名字的 BeanDefinition
    private boolean allowBeanDefinitionOverriding = true;

    // 对于lazy-init bean, 是否也允许立即加载
    private boolean allowEagerClassLoading = true;

    // 用于检查 bean definition 是否是自动装配候选者的解析器。
    private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

    private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

    private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    // List of names of manually registered singletons, in registration order.
    // 按照注册的顺序, 手动的注册单例的名称列表
    private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

}

```


DefaultListableBeanFactory.preInstantiateSingletons bean 的实例化

```java
public void preInstantiateSingletons() throws BeansException {

    // 获取当前所有的 beanName
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // Trigger initialization of all non-lazy singleton beans...

    // 从 Map<String, RootBeanDefinition> mergedBeanDefinitions 中获取, 获取不到进行创建
    for (String beanName : beanNames) {


        // 先从 Map<String, RootBeanDefinition> mergedBeanDefinitions 中通过 beanName 获取,
        // 获取不到的话
        // 1. 从容器中的 缓存中获取这个 beanName 对应的 BeanDefinition
        // 2. 获取获取到的 BeanDefintion 的 parentName
        // 2.1 parentName 为空, 表示这个 beanDefintion 自身就是父级了
        // 2.1.1 这个 BeanDefintion 是否为 RootBeanDefinition 的子类
        // 2.1.1.1 是的话，强转为 RootBeanDefinition, 调用自身的 cloneBeanDefinition 方法 (return new RootBeanDefinition(this)), 得到一个新的 RootBeanDefinition
        // 2.1.1.2 不是, mbd = new RootBeanDefinition(bd); 构造函数内部会逐步获取 bd 的属性设置到 RootBeanDefinition 对应的属性

        // 2.2 parentName 不为空, 表示这个 beanDefinition 是有父级的, 需要合并父级的属性

        // 2.2.1 parentName 是否等于 beanName, 如果是的话，会尝试从这个 BeanFactory 获取其父级 BeanFactory, 同时这个 父级的 BeanFactory 必须为 ConfigurableBeanFactory 子类, 否则就报错
        // 2.2.1.1 强转父级为 ConfigurableBeanFactory, 调用其 getMergedBeanDefinition 得到一个新的 父级的 BeanDefinition

        // 2.2.2 parentName 不等于 beanName, 调用  getMergedBeanDefinition 得到一个父级的 BeanDefinition

        // 2.2.3 通过 new RootBeanDefinition(Bean Definition) 将父级的 BeanDefinition 转为一个新的 RootBeanDefinition
        // 2.2.4 调用 RootBeanDefinition 的 overrideFrom 方法把 我们子类的 BeanDefinition 的属性拷贝过去, 得到一个完整的 子类合并父类属性的 RootBeanBefinition

        // parentName 就是类似于继承，抽象类一样, 把相同的配置声明为 parentName，子类自定义，然后设置 parentName 为这个，就能达到配送形式的继承。

        // 3. 新的 RootBeanDefinition 的 scope 是否有配置, 没有的话设置默认置为 singleton

        // 4. 入参的第二个参数 BeanDefinition containingBd 不为空, containingBd 不是单例，而新的  RootBeanDefinition 为单例，设置新的 RootBeanDefinition 的 scope = containingBd 的 scpoe
        // 兼容非单例的bean, 单例的 bean 包含非非单例的bean, 就不会是单例的

        // 5. 把最新的 RootBeanDefinition 放入到缓存 Map<String, RootBeanDefinition> mergedBeanDefinitions
        // 6. 返回最新的 RootBeanDefinition


        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        // bean 配置的是单例, 不是 lazy-init 和 Abstract
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            
            // 第一步，调用 getSingleton() 得到这个 beanName 对应的对象

            // 1. 从已经创建的的单例缓存 Map<String, Object> singletonObjects 获取这个 beanName 的 bean
            // 1.1 获取到的这个 bean 为空, 
            // 1.1.1 当前正在创建的的 beanName 缓存 Set<String> singletonsCurrentlyInCreation 包含这个 beanName, 如果不包含直接返回 null
            // 1.1.2 从 Map<String, Object> earlySingletonObjects 中获取这个 beanName 对应的 bean, 获取到了 返回这个 bean
            // 1.1.3 获取不到，是否允许提前引用, 不允许返回 null
            // 1.1.4 允许提前引用, 对 Map<String, Object> singletonObjects 加锁,
            // 1.1.5 singletonObjects 再次尝试从 Map<String, Object> singletonObjects 中获取这个 beanName 对应的 bean, 获取到了 返回这个 bean
            // 1.1.6 获取不到, 同样再次 从 Map<String, Object> earlySingletonObjects 中获取这个 beanName 对应的 bean, 获取到了 返回这个 bean
            // 1.1.7 还是获取不到, 从 Map<String, ObjectFactory<?>> singletonFactories 中获取我们的 beanName 对应的对象 bean
            // 1.1.8 把获取到的 beanName 对应的 bean, 放入 Map<String, Object> earlySingletonObjects 中, 
            // 1.1.9 从 singletonFactories 中移除这个 beanName 对应的对象
            // 1.1.10 返回这个 bean

            // 1.2 获取到的这个 bean 不为空, 直接返回这 bean

            // 第二步, 第一步获取的对象不为空, 判断是否为 FactoryBean, 是返回 true, 否返回 false

            // 第三步, 第一步获取的对象为空
            // 1. 当前的 beanDefinition 缓存 Map<String, BeanDefinition> beanDefinitionMap 包含了这个 beanName吗
            // 1.1 不包含, 但是当前的 BeanFactory 的父级为 ConfigurableBeanFactory 子类, 获取其父级, 调用父级的 isFactoryBean 进行判断

            // 1.2 包含, 不包含且父级不为 ConfigurableBeanFactory 的子类
            // 1.2.1 调用 getMergedLocalBeanDefinition 获取当前 beanName 对应的 RootBeanDefinition
            // 1.2.2 调用 isFactoryBean(String beanName, RootBeanDefinition mbd) 进一步判断
            // 1.2.2.1 获取当前的 beanName 的类型, 判断是否为 BeanFactory

            // 是否为 FactoryBean 的子类
            if (isFactoryBean(beanName)) {
                // beanName 前面加个 &, 获取这个 新 BeanName 对应的 bean
                Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);

                if (bean instanceof FactoryBean) {
                    FactoryBean<?> factory = (FactoryBean<?>) bean;

                    boolean isEagerInit;

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
            } else {
                // 获取 bean
                getBean(beanName);
            }

        }


        for (String beanName : beanNames) {
            Object singletonInstance = getSingleton(beanName);
            // 单例为 SmartInitializingSingleton 子类
			if (singletonInstance instanceof SmartInitializingSingleton) {
                SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;

                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {

                        smartSingleton.afterSingletonsInstantiated();
						return null;

                    }, getAccessControlContext());

                } else {
                    // 执行这个对象的  afterSingletonsInstantiated 方法
                    smartSingleton.afterSingletonsInstantiated();
                }
            }
        }
    }
}
```

```java

public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, null, false);
}


protected <T> T doGetBean(String name, Class<T> requiredType, Object[] args, boolean typeCheckOnly) throws BeansException {

    String beanName = transformedBeanName(name);
    Object bean;

    Object sharedInstance = getSingleton(beanName);
    // 检查单例缓存是否有手动注册的单例
    if (sharedInstance != null && args == null) {


        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);

    } else {

        // 判断 ThreadLocal<Object> prototypesCurrentlyInCreation 的值是否为当前的 beanName
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        // 获取当前的 BeanFactory 的父级
        BeanFactory parentBeanFactory = getParentBeanFactory();

        // 有父级的 BeanFactory, 当前的 BeanDefinition 缓存包含这个 beanName
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // 获取原来的名字
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

        if (!typeCheckOnly) {
            // 已经创建的 bean Set<String> alreadyCreated 如果已有这个 beanName, 跳过
            // 没有的话
            // 1. Map<String, RootBeanDefinition> mergedBeanDefinitions 加锁,
            // 2. 然后移除这个缓存里面的 beanName
            // 3. Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders 同样移除这个的 beanName
            // 4. 向 Set<String> alreadyCreated 中添加这个 beanName
            markBeanAsCreated(beanName);
        }

        try {

            // 获取这个beanName 对应的 RootBeanDefinition
            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            // 检查这个 bean 是否可以创建，方法内部只是判断了 mbd 是否为抽象类 mbd.isAbstract() 是的话, 抛异常
            checkMergedBeanDefinition(mbd, beanName, args);

            // 获取这个 mdb 依赖的对象 beanName
            String[] dependsOn = mbd.getDependsOn();

            if (dependsOn != null) {
                for (String dep : dependsOn) {
                    // 1. 对 Map<String, Set<String>> dependentBeanMap 加锁
                    // 2. 获取这个 beanName 实际的 beanName, （可能入参的这个 beanName 为别名）
                    // 3. 从 Map<String, Set<String>> dependentBeanMap 获取 beanName 依赖的集合 Set<String> dependentBeans
                    // 4. Set<String> dependentBeans 为空, 返回 false
                    // 5. Set<String> dependentBeans 包含依赖的 beanName, 返回 true
                    // 6. 遍历 Set<String> dependentBeans, 判断里面的每一项是否依赖 dep
                    // 7. 主要是判断间接依赖的情况, 如 入参1 为A, 入参2 为 B, 找到 A 依赖的 beanName 为 C,D, 虽然 A 不直接依赖 B, 但是可能 C, D 依赖于 B

                    // 判断 dep 是否整点依赖于 beanName, 循环依赖判断
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }

                    // 向 Set<String> dependentBeans 注册当前的依赖关系, 反方向的,机 dep 被 beanName 依赖
                    // 同时向 Map<String, Set<String>> dependenciesForBeanMap 注册 beanName 依赖于 dep
                    registerDependentBean(dep, beanName);

                    try {
                        // 获取依赖的 bean
                        getBean(dep);
                    } catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                    }
                }
            }

            // 单例 bean
            if (mbd.isSingleton()) {
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
                //  Prototype 创建一个新的对象
                Object prototypeInstance = null;

                try {
                    beforePrototypeCreation(beanName);
                    prototypeInstance = createBean(beanName, mbd, args);
                } finally {
                    afterPrototypeCreation(beanName);
                }

                // 从当前的线程共享 NamedThreadLocal<String> currentlyCreatedBean 中获取当前正在创建的 beanName
                // 获取到的 beanName2 不为空，
                // 1. 向容器的依赖 Map<String, Set<String>> dependentBeanMap 中 beanName 依赖这个 beanName2
                // 2. 向容器的依赖 Map<String, Set<String>> dependenciesForBeanMap 注册这个 beanName2 的依赖 beanName

                // 3. 调用父级的 getObjectForBeanInstance 方法
                // 3.1 当前的 beanName 是否为 工厂引用 (以 & 开头)
                // 3.1.1 是的话, 当前的 bean 实例为 NullBean, 直接返回
                // 3.1.2 是的话, 当前的 bean 不是 FactoryBean 的实例， 抛异常
                // 3.1.3 不是的话, 继续

                // 4 当前的 bean 不是 FactoryBean 的实例, 或者 beanName 以 & 开头, 直接返回 bean 实例

                // 直接声明 bean 的结束，下面的是 FactoryBean 的逻辑

                // 5. 声明 对象 object
                // 5.1 如果 BeanDefinition 为空, 从 FactoryBean 缓存中  Map<String, Object> 获取对应的 bean, object 等于这个 bean
                // 5.2 如果 object 不为空， 返回这个对象 object

                // 5.3 强转 beanInstance为 FactoryBean<?>
                // 6.2 如果 BeanDefintion 为空同时 BeanDefinition 缓存包含这个 beanName,  获取这个 beanName 对应的 BeanDefintion
                // 6.3 获取到的 BeanDefintion 不为空同时 这个 BeanDefinition 不是合成的,  前面的是否 赋值给标识 synthetic
                // 6.4 通过 getObjectFromFactoryBean 获取对象， 既通过强转的实例 FactoryBean<?> 获取 beanName 对应的对象，  入参中有一个为 !synthetic， 
                
                // 7. getObjectFromFactoryBean 的逻辑
                // 8. FactoryBean 为单例 同时 单例缓存 Map<String, Object> singletonObjects 中包含这个 beanName
                // 8.1 对 singletonObjects 加锁
                // 8.2 再次从 Map<String, Object> 中获取 beanName 对应的 bean 实例
                // 8.3 如果获取的 bean 实例不为空，返回

                // 8.4 bean 实例为空
                // 8.5 调用 FactoryBean 的 getObject 方法, 获取实例对象
                // 8.6 获取到的对象为空, 判断当前正在创建的对象缓存中包含这个 beanName, 包含抛异常, 不包含, 返回 获取的实例对象赋值为 NullBean

                // 8.7 再次从 Map<String, Object> factoryBeanObjectCache 中获取这个 beanName 对应的缓存对象，不为空, 赋值给上一步的实例对象
                // 8.8 获取的缓存对象为空, 判断入参的 shouldPostProcess 是否需要后置处理
                // 8.9 如果为 false, 不做处理
                // 8.10 为 true, 判断当前正在创建的对象缓存中包含这个 beanName, 包含直接返回这个实例对象
                
                // 8.11 创建检查排查列表 Set<String> inCreationCheckExclusions 中不包含这个 beanName, 想正在创建的列表中添加这个 beanName, 成功
                // 8.12 执行自身的 postProcessObjectFromFactoryBean 方法
                // 8.13 从正在创建的列表中移除这个 beanName 

                // 8.14 当前的单例缓存列表  Map<String, Object> singletonObjects 中包含这个 beanName 的实例, 将其放入缓存  Map<String, Object> factoryBeanObjectCache 中
                // 8.15 返回最终的对象

    
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            } else {

                // 其他类型的话, 也是直接创建
                String scopeName = mbd.getScope();
                if (!StringUtils.hasLength(scopeName)) {
                    throw new IllegalStateException("No scope name defined for bean ´" + beanName + "'");
                }
                Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }

                try {
                    Object scopedInstance = scope.get(beanName, () -> {
                        beforePrototypeCreation(beanName);
                        try {
                            return createBean(beanName, mbd, args);
                        } finally {
                            afterPrototypeCreation(beanName);
                        }
                    }
                } catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName, "Scope '" + scopeName + "' is not active for the current thread; consider " +
                            "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                            ex);
                }
            }

        } catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }

    }

    // 指定了 bean 的类型, 同时获取的 bean 不是指定类型的实例
    if (requiredType != null && !requiredType.isInstance(bean)) {
        try {
			T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);

            if (convertedBean == null) {
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            return convertedBean;
        } catch (TypeMismatchException ex) {
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }

    return (T) bean;
}

```


```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {

    // 给单例缓存对象加锁
    synchronized (this.singletonObjects) {
        
        // 从 单例缓存对象中获取单例对象
        Object singletonObject = this.singletonObjects.get(beanName);

        if (singletonObject == null) {
            // 一般为 false, 销毁 bean 单例时, 会先将其置为 false
			if (this.singletonsCurrentlyInDestruction) {
                throw new BeanCreationNotAllowedException(beanName, "Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
            }

            // 在 bean 实例之前
            // 判断 Set<String> inCreationCheckExclusions 是否包含当前的 beanName,  不需要检查的 Set 列表不包含这个 beanName
            // 并且 向正在创建的 beanName 列表 Set<String> singletonsCurrentlyInCreation  添加这个 beanName 失败, 则会抛出异常
            beforeSingletonCreation(beanName);

            boolean newSingleton = false;

            // private Set<Exception> suppressedExceptions; 
            // 不影响流程的异常列表 == null, 有异常就结束
            boolean recordSuppressedExceptions = (this.suppressedExceptions == null);

            // 赋值后会在 finally 在清空, 除非一开始就不是空的
            if (recordSuppressedExceptions) {
				this.suppressedExceptions = new LinkedHashSet<>();
			}

            try {
                
                // 通注入的 lamdba 表达式获取单例对象
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

            if (newSingleton) {

                // 同样对 this.singletonObjects 进行加锁
                // 1. 向 this.singletonObjects 添加这个单例对象
                // 2. Map<String, ObjectFactory<?>> 中移除这个 beanName
                // 3. Map<String, Object> earlySingletonObjects 中移除这个 beanName
                // 4. Set<String> registeredSingletons 已经注册的单例列表中添加这个 beanName
                addSingleton(beanName, singletonObject);
            }
        }

        return singletonObject;
    }

}

```

```java

protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {

    RootBeanDefinition mbdToUse = mbd;
    // 获取 bean 的 class 类型
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);

    // mbd.hasBeanClass 既 mbd 里面的 Object beanClass 为 Class 类型

    // 确保 bean 类在此时被实际解析, 如果动态解析的 class 不能存储在共享 合并 beanDefinition 中, 则克隆 BeanDefinition。
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        // 深拷贝一个新的 RootBeanDefinition 
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    try {
        // 处理 beanDefinition 中的 MethodOverrides 即处理  lookup-method 和 replaced-method 2 种情况

        // 遍历所有的 MethodOverrides methodOverrides 中所有的 Set<MethodOverride>, 如果里面 MethodOverride 中的方法名在类中存在 0 个, 抛异常, 等于 1 个, 设置 MethodOverride 的 overloaded 为 false， 默认为 true
        // 避免参数类型检查的开销
        mbdToUse.prepareMethodOverrides();
    } catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(), beanName, "Validation of method overrides failed", ex);
    }

    try {

        // 让 BeanPostProcessors 有机会返回一个代理而不是目标 bean 实例    
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);

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

@Nullable
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {

    Object bean = null;

    // 包可见属性, 表示
    // 如果尚未被解析  默认为 null 下面会在解析后，将其变为 true
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {

        // 这个 mbd 是否为 Spring 自身生成的, 比如 aop 代理生成， 默认为 false,
        //返回 this.hasInstantiationAwareBeanPostProcessors 的值, 默认为 false, 当容器中有 InstantiationAwareBeanPostProcessor 类型的 BeanPostProcessor 会变为 true
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = determineTargetType(beanName, mbd);
            if (targetType != null) {
                bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                if (bean != null) {
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }
        // 可能会变为 fasle
        mbd.beforeInstantiationResolved = (bean != null);
    }
    return bean;
}






protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {

    BeanWrapper instanceWrapper = null;

    if (mbd.isSingleton()) {
        // 未完成的 FactoryBean 缓存 ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache 中删除这个 beanName 对应的数据
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
    	instanceWrapper = createBeanInstance(beanName, mbd, args);
	}

    Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                // 如果 List<BeanPostProcessor> beanPostProcessors 中包含 MergedBeanDefinitionPostProcessor, 依次调用 MergedBeanDefinitionPostProcessor 的 postProcessMergedBeanDefinition 方法
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            } catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Post-processing of merged bean definition failed", ex);
            }
            // 后置处理变为 true
            mbd.postProcessed = true;
        }
    }

    // 单例 + 配置的允许自动解决在 bean 之间的循环引用 + 当前的 bean 已经在正则创建的 beanName 集合 Set<String> singletonsCurrentlyInCreation 中
    // 热切地缓存单例对象来解析循环引用,  即使是由生命周期接口触发的，例如 BeanFactoryAware
    // 一般情况为 true
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName));

    if (earlySingletonExposure) {
        // 对单例集合 Map<String, Object> singletonObjects 上锁
        // 如果单例集合 Map<String, Object> singletonObjects 还是不包含这个 beanName
        // 1. 向 Map<String, ObjectFactory<?>> singletonFactories 添加这个 beanName 对应的获取 bean 对象的箭头函数
        // 2. 从 Map<String, Object> earlySingletonObjects 早期创建对象集合中移除这 beanName
        // 3. 向 Set<String> registeredSingletons 中登记这个 beanName
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }
    
    Object exposedObject = bean;

    try {
        // 填充 bean
		populateBean(beanName, mbd, instanceWrapper);
    } catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
            throw (BeanCreationException) ex;
        } else {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
        }
    }

    if (earlySingletonExposure) {
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    Object exposedObject = bean;
    try {
        populateBean(beanName, mbd, instanceWrapper);

        // 真正的添加 bean 属性

        // 1. 填充各个 Aware 接口的值 BeanNameAware  BeanClassLoaderAware  BeanFactoryAware
        
        // 2. 依次调用各个 BeanPostProcessor 的 postProcessBeforeInitialization 方法 
        // 2.1  ApplicationContextAwareProcessor 这个 BeanProcessor 会填充其他的 Aware 
        // EnvironmentAware EmbeddedValueResolverAware ResourceLoaderAware ApplicationEventPublisherAware MessageSourceAware ApplicationContextAware
        
        // 3. 当前的 bean 是 InitializingBean 的实例, 同时 这个bean 对应的 BeanDefinition 为空 或者 BeanDefinition 自定义的 init-method 不包含  afterPropertiesSet
        // 3.1 则调用 这个 bean 的 afterPropertiesSet 方法

        // 4. BeanDefinition 不为 null, bean 的 class 不是 NULLBean
        // 4.1 BeanDefinition 的 初始方法名 init—method 有配置的话, 调用配置的 init-methods 方法

        // 5. BeanDefinition 为空 或者 BeanDefinition 不是 Synthetic 合成的
        // 5.1 遍历所有的 BeanPostProcessor, 调用他们的 postProcessAfterInitialization 方法
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    } catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
			throw (BeanCreationException) ex;
		}
		else {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
		}
    }

    if (earlySingletonExposure) {

        // 从单例缓存中尝试获取这个 beanName 对象
		Object earlySingletonReference = getSingleton(beanName, false);

        // 存在
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                // 同一个对象
                exposedObject = earlySingletonReference;

            // allowRawInjectionDespiteWrapping 默认为 false,  Map<String, Set<String>> dependentBeanMap 包含这个 beanName 的依赖    
            } else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                // 获取依赖的 beanName 
                String[] dependentBeans = getDependentBeans(beanName);
				Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    // 如果已经创建的对象中包含这个 beanName
                    // 1. 从单例缓存中移除这个 beanName 的实例
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }

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

        // BeanDefinition 不是 prototype 范围, 同时需要销毁的
        
        // 需要消耗的判断条件

        // 1. 需要销毁的 bean 不是 NullBean 类型
        // 2.1 DisposableBean 和  AutoCloseable 的子类
        // 2.2 设置了 destroy—Method 方法
        // 2.3 表示 容器中有 DestructionAwareBeanPostProcessor 的实例的标识 hasDestructionAwareBeanPostProcessors 为 true 并且 DestructionAwareBeanPostProcessor 的实例集合对这个 bean 的需要执行消耗逻辑

        // 3. 1 和 2 中的任意一点为 true, 走下面的逻辑

        // 4. 当前 bean 为 单例
        // 4.1 想容器中的 Map<String, Object> disposableBeans 注册这个 beanName 对应的销毁类 DisposableBean (默认实现为 DisposableBeanAdapter 实现)
        // 4.2 DisposableBeanAdapter 默认的执行 destory 方法的顺序 
        // 4.2.1  (1) 所有 DestructionAwareBeanPostProcessor 实例的 postProcessBeforeDestruction 方法
        // 4.2.2  (2) 当前 bean 为 DisposableBean 的子类， 调用自身的 destory 方法
        // 4.2.3  (3) 调用自身配置的 init-destory 方法

        // 5. 当前 bean 不是单例
        // 5.1 从容器的 Map<String, Scope> scopes 中获取这个 bean 对应的 Scope, 向这个 Scope 注册这个bean 对应的 销毁类 DisposableBean (默认实现为 DisposableBeanAdapter 实现)

        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    } catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }
    return exposedObject;
}


protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    if (bw == null) {
        // bean 对象为空, 但是 beanDefinition 有属性要填充
        if (mbd.hasPropertyValues()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
        }

    } else {
        return;
    }

    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {

                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    return;
                }
            }
        }
    }

    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
    
    // 获取注入模式
    int resolvedAutowireMode = mbd.getResolvedAutowireMode();

    if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

        // 通过属性名注入， 填充 newPvs 对象
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }

        // 通过类型注入, 填充 newPvs 对象
        if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }
        pvs = newPvs;

    }

    // 是否有 InstantiationAwareBeanPostProcessors 的实例
    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    // 是否需要依赖检查
    boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

    if (hasInstAwareBpps) {

        if (pvs == null) {
            pvs = mbd.getPropertyValues();
        }

        for (BeanPostProcessor bp : getBeanPostProcessors()) {

            // 特殊处理 InstantiationAwareBeanPostProcessor
        	if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
				PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);

                if (pvsToUse == null) {
                    if (filteredPds == null) {
                        filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                    }

                    pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);

                    if (pvsToUse == null) {
                        return;
                    }
                }
                pvs = pvsToUse;
            }
        }

    }

    // 需要依赖检查
    if (needsDepCheck) {
        if (filteredPds == null) {
            // 过滤出需要依赖检查的属性
            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
        }

        checkDependencies(beanName, mbd, filteredPds, pvs);
    }

    if (pvs != null) {
        // 填充属性到 bw 这个对象中
        applyPropertyValues(beanName, mbd, bw, pvs);
    }

}



```


```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {

    // 获取 bean 的类型
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();

    // 通过内置的 Supplier 创建 实例, 一般为 null
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    // 工厂方法的话，通过工厂方法进行创建
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // 重新创建同一个 bean 的快照接近
    boolean resolved = false;
	boolean autowireNecessary = false;

    if (args == null) {
        synchronized (mbd.constructorArgumentLock) {
            // 有构造函数或者工厂方法
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
				autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
    }

    if (resolved) {
        if (autowireNecessary) {
            return autowireConstructor(beanName, mbd, null, null);
        }
        else {
            return instantiateBean(beanName, mbd);
        }
    }

    // SmartInstantiationAwareBeanPostProcessor 是否有的实例, 有调用他的 determineCandidateConstructors 方法，得到一个构造函数
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR || mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // 首选的构造函数
    ctors = mbd.getPreferredConstructors();

    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null);
    }

    return instantiateBean(beanName, mbd);
}

protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {

    try {

        Object beanInstance;

        if (System.getSecurityManager() != null) {
            beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () -> getInstantiationStrategy().instantiate(mbd, beanName, this), getAccessControlContext());
        } else {
            // 获取容器的实例化策略对象，进行实例化, 默认为 CglibSubclassingInstantiationStrategy, 但是后面可能被修改为 SimpleInstantiationStrategy
            beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
        }

        BeanWrapper bw = new BeanWrapperImpl(beanInstance);
        initBeanWrapper(bw);
        return bw;

    } catch(Throwable ex) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
    }


}
```


SimpleInstantiationStrategy

```java

public class SimpleInstantiationStrategy implements InstantiationStrategy {

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {

        if (!bd.hasMethodOverrides()) {
            Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {
                constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
                    final Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}

                    try {
						if (System.getSecurityManager() != null) {
                            constructorToUse = AccessController.doPrivileged((PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor););
                        } else {
                            // 构造函数
                            constructorToUse = clazz.getDeclaredConstructor();
                        }

                        bd.resolvedConstructorOrFactoryMethod = constructorToUse;
                    } catch (Throwable ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
                }

            }
            // 通过构造函数创建出对象
            return BeanUtils.instantiateClass(constructorToUse);
        } else {
            // 在 SimpleInstantiationStrategy 中不支持 方法注入的形式, 直接报错
            return instantiateWithMethodInjection(bd, beanName, owner);
        }
    }

    
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}
}

```


参考

[怎么阅读Spring源码？](https://www.zhihu.com/question/21346206)
[spring加载bean流程解析](https://www.cnblogs.com/wyq178/p/11415877.html)