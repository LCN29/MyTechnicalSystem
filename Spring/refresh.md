
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

    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // Trigger initialization of all non-lazy singleton beans...

    // 从 Map<String, RootBeanDefinition> mergedBeanDefinitions 中获取, 获取不到进行创建
    for (String beanName : beanNames) {

        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        // bean 配置的是单例, 不是 lazy-init 和 Abstract
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {


        }

    }

}


```