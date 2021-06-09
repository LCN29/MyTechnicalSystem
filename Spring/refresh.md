
https://www.zhihu.com/question/21346206

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