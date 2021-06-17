
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
            // 单例为 SmartInitializingSingleton 子类  in ni su 来,真(潮)
			if (singletonInstance instanceof SmartInitializingSingleton) {
                SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;

                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {

                        smartSingleton.afterSingletonsInstantiated();
						return null;

                    }, getAccessControlContext());

                } else {
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

    // TODO 

    if (requiredType != null && !requiredType.isInstance(bean)) {


    }



}

```


```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {

    // 给单例缓存对象加锁
    synchronized (this.singletonObjects) {

        Object singletonObject = this.singletonObjects.get(beanName);

        if (singletonObject == null) {
			if (this.singletonsCurrentlyInDestruction) {

            }
        }

    }

}

```