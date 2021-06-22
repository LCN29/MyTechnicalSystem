# Spring Bean 生命周期


```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    private final Object startupShutdownMonitor = new Object();

    private long startupDate;

    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicBoolean active = new AtomicBoolean();

    private ConfigurableEnvironment environment;

    private Set<ApplicationListener<?>> earlyApplicationListeners;
    
    private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

    private Set<ApplicationEvent> earlyApplicationEvents;
}

```

```java
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    private volatile DefaultListableBeanFactory beanFactory;

    private Boolean allowBeanDefinitionOverriding;

    private Boolean allowCircularReferences;

}
```

```java
/**
 * 这里的 ConfigurableListableBeanFactory 为自定义的类
 * 是对 BeanFactory 的这个继承关系的合并, 在 Spring 的实现中比这个复杂多
 */
public class ConfigurableListableBeanFactory {

    private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories = new ConcurrentHashMap<>(8);

    private String serializationId;

    private boolean allowBeanDefinitionOverriding = true;

    private boolean allowEagerClassLoading = true;

    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    private BeanExpressionResolver beanExpressionResolver;

    private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    private volatile boolean hasInstantiationAwareBeanPostProcessors;

    private volatile boolean hasDestructionAwareBeanPostProcessors;

    private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    private ClassLoader tempClassLoader;

    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

    private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

    private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

    /** Map of singleton and non-singleton bean names, keyed by dependency type. */
    private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

    /** Map of singleton-only bean names, keyed by dependency type. */
    private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);
    

}
```
















Spring Bean 分为 2 类
> 1. 普通的 Bean
> 2. 特殊的 Bean


