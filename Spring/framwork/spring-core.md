# 从配置文件中加载 bean

```java
public ClassPathXmlApplicationContext (String[] configLocations, boolean refresh, ApplicationContext parent) {

	// 第一步
	super(parent);

	// 第二步
	setConfigLocations(configLocations);

	// 第三步 默认为 true
	if (refresh) {
		refresh();
	}
}
```

第一步: 一直调用到 AbstractApplicationContext 的构造函数, 内部做了
>1. 初始自身的  ResourcePatternResolver resourcePatternResolver 属性, 具体的实现为 PathMatchingResourcePatternResolver, 方法

```java
public AbstractApplicationContext() {
	this.resourcePatternResolver = getResourcePatternResolver();
}

protected ResourcePatternResolver getResourcePatternResolver() {
	return new PathMatchingResourcePatternResolver(this);
}
```

可以发现方法 getResourcePatternResolver 方法是 protected 的，子类可以进行自己的实现, 声明了 ResourcePatternResolver 用于后面的配置文件解析


>2. 父级应用设置, 因为调用的时候 parent 为 null, 所以下面的逻辑基本不会走到
```java
public void setParent(@Nullable ApplicationContext parent) {
	this.parent = parent;
	if (parent != null) {
		Environment parentEnvironment = parent.getEnvironment();
		if (parentEnvironment instanceof ConfigurableEnvironment) {
			getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
		}
	}
}
```


第二步: 调用自身的 setConfigLocations 方法，主要是对传入的配置文件进行解析，并放到 configLocations 这个数组中
配置文件支持 "${配置}/xxx.config" 主要是真的 ${} 进行解析

解析的过程中，会进行 ConfigurableEnvironment 的初始，通过调用 AbstractApplicationContext 的 createEnvironment 方法(protected 方法), 创建出当前的环境, 默认为 StandardEnvironment
然后调用 Environment 的 resolveRequiredPlaceholders(String text) 方法对传入的路径进行解析

第三步: 终于到了重点的 refresh()

>1. 先直接对自身的`Object startupShutdownMonitor` 加锁, 确保同一时刻同一个 AbstractApplicationContext 只有一个线程在执行 refresh 方法

>2. 调用自身的 prepareRefresh 方法(protected 方法)
>>2.1 记录一下 Application 启动的时间，存放在 long startupDate
>>2.2 设置当前 Applicaton 的 2 个状态值，是否关闭状态: false, 激活状态: true
>>2.3 调用自身的 initPropertySources 方法(protected 方法), 自身没有实现，子类可以进行重写, 进行属性的一下初始
>>2.4 调用当前的 ConfigurableEnvironment environment 属性的 validateRequiredProperties 方法, 实际调用到了 AbstractEnvironment的 validateRequiredProperties 方法, 最终调用到了 PropertySourcesPropertyResolver.validateRequiredProperties 方法对必须要的属性进行必要校验
>>2.5 把前期注册的监听器放到 earlyApplicationListeners 中
>>2.6 声明应用事件监听事件列表 Set<ApplicationEvent> earlyApplicationEvents

>3. 调用自身的 obtainFreshBeanFactory 方法, 得到一个 BeanFactory 
>>3.1 判断当前的 DefaultListableBeanFactory BeanFactory 是否为空, 不会空的话，进行 BeanFactory 的 destroySingletons 进行销毁，同时将当前的 BeanFactory 置为空
>>3.2 创建新的 DefaultListableBeanFactory 
>>3.3 设置当前 DefaultListableBeanFactory 的序列化id (默认为类名+ @ + 当前实例的 hashCode 的 16 进制)
>>3.4 自定义 DefaultListableBeanFactory 的属性 1: 注册时同名的是否可以进行覆盖(默认为true), 2: 是否允许循环依赖, 尽可能的尝试解决循环依赖(默认为true)

```java
public void refresh() throws BeansException, IllegalStateException {

	synchronized (this.startupShutdownMonitor) {
		
		prepareRefresh();
		ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
		prepareBeanFactory(beanFactory);

		try {
			postProcessBeanFactory(beanFactory);
			invokeBeanFactoryPostProcessors(beanFactory);
			registerBeanPostProcessors(beanFactory);
			initMessageSource();
			initApplicationEventMulticaster();
			onRefresh();
			registerListeners();
			finishBeanFactoryInitialization(beanFactory);
			finishRefresh();
		} catch (BeansException ex) {

			if (logger.isWarnEnabled()) {
				logger.warn("Exception encountered during context initialization - cancelling refresh attempt: " + ex);
			}
			destroyBeans();
			cancelRefresh(ex);
			throw ex;
		} finally {
			resetCommonCaches();
		}
	}
}
```