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


>1. 先直接对自身的`Object startupShutdownMonitor` 加锁, 确保同一时刻同一个 AbstractApplicationContext 只有一个线程在执行 refresh 方法

>2. 调用自身的 prepareRefresh 方法(protected 方法)
>>2.1 记录一下 Application 启动的时间，存放在 long startupDate
>>2.2 设置当前 Applicaton 的 2 个状态值，是否关闭状态: false, 激活状态: true
>>2.3 调用自身的 initPropertySources 方法(protected 方法), 自身没有实现，子类可以进行重写, 进行属性的初始
>>2.4 调用当前的 ConfigurableEnvironment environment 属性的 validateRequiredProperties 方法, 实际调用到了 AbstractEnvironment的 validateRequiredProperties 方法, 最终调用到了 PropertySourcesPropertyResolver.validateRequiredProperties 方法对必须要的属性进行必要校验， 可以通过  requiredProperties 进行设置
>>2.5 把前期注册的监听器放到 earlyApplicationListeners 中
>>2.6 声明应用事件监听事件列表 Set<ApplicationEvent> earlyApplicationEvents

>3. 调用自身的 obtainFreshBeanFactory 方法, 得到一个 ConfigurableListableBeanFactory


第一步 进行 BeanFactory 的检测, 调用到了 refreshBeanFactory， 默认会调用到 AbstractRefreshableApplicationContext.refreshBeanFactory 的方法

>>3.1 判断当前的 DefaultListableBeanFactory BeanFactory 是否为空, 不会空的话，进行 BeanFactory 的 destroySingletons 进行销毁，同时将当前的 BeanFactory 置为空
>>3.2 创建新的 DefaultListableBeanFactory, 创建的过程会获取 父级 parent 的 BeanFactory，存放到 自身的 BeanFactory parentBeanFactory 中, 没有则为 null
>>3.3 设置当前 DefaultListableBeanFactory 的序列化id (默认为类名+ @ + 当前实例的 hashCode 的 16 进制)
>>3.4 自定义 DefaultListableBeanFactory 的属性 1: 注册时同名, 是否可以进行覆盖(默认为true), 2: 是否允许循环依赖, 尽可能的尝试解决循环依赖(默认为true)

>>3.5 调用 (DefaultListableBeanFactory beanFactory) AbstractXmlApplicationContext 的方法, 这个是 bean 的加载的核心

1. 声明 XmlBeanDefinitionReader, 将 beanFactory 作为参数传入
2. 依次设置 XmlBeanDefinitionReader 的 Environment, ResourceLoader, EntityResolver 
3. 调用自身的 protected 方法 initBeanDefinitionReader，设置 XmlBeanDefinitionReader 的 校验(validation) 模式，默认为 自动校验 VALIDATION_AUTO
4. 调用 loadBeanDefinitions 加载 bean

```java
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {

	// 获取 Resource
	Resource[] configResources = getConfigResources();
	if (configResources != null) {
		reader.loadBeanDefinitions(configResources);
	}

	// 获取需要解析的路径, 存在, 调用 XmlBeanDefinitionReader 进行 bean 的加载
	String[] configLocations = getConfigLocations();
	if (configLocations != null) {
		// 调用到了 AbstractBeanDefinitionReader 的 loadBeanDefinitions
		reader.loadBeanDefinitions(configLocations);
	}
}
```


```java
/**
 * 第二个参数为 空
 */ 
public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {

	// 调用资源加载， ClassPathXmlApplicationContext 实现了这个接口
	ResourceLoader resourceLoader = getResourceLoader();
	if (resourceLoader == null) {
		throw new BeanDefinitionStoreException(
				"Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
	}

	if (resourceLoader instanceof ResourcePatternResolver) {
		// Resource pattern matching available.
		try {
			// 会调用到 AbstractApplicationContext 的 getResources 方法，方法的实现是通过自身的  ResourcePatternResolver 对象的 getResource 方法
			Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
			int count = loadBeanDefinitions(resources);
			if (actualResources != null) {
				Collections.addAll(actualResources, resources);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
			}
			return count;
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Could not resolve bean definition resource pattern [" + location + "]", ex);
		}
	}
	else {
		// Can only load single resources by absolute URL.
		// 解析出需要的加载的资源
		Resource resource = resourceLoader.getResource(location);
		int count = loadBeanDefinitions(resource);
		if (actualResources != null) {
			actualResources.add(resource);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
		}
		return count;
	}
}


// PathMatchingResourcePatternResolver 的 getResource 方法

@Override
public Resource[] getResources(String locationPattern) throws IOException {
    
	Assert.notNull(locationPattern, "Location pattern must not be null");

	// 路径以 classpath*: 开头
	if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
		// a class path resource (multiple resources for same name possible)
		if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
			// a class path resource pattern
			return findPathMatchingResources(locationPattern);
		}
		else {
			// all class path resources with the given name
			return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
		}
	}
	else {

		// Generally only look for a pattern after a prefix here,
		// and on Tomcat only after the "*/" separator for its "war:" protocol.
		
		// 查找路径前缀结束的位置， 除了支持常用的 classpath 还支持 Tomcat 的 war: 协议
		int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
				locationPattern.indexOf(':') + 1);

		// 调用到 AntPathMatcher 的 isPattern 方法， 截掉了前缀部分
		// 判断去掉前缀后, 是否包含 * 和 {} 			
		if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
			// a file pattern
			return findPathMatchingResources(locationPattern);
		}
		else {
			// a single resource with the given name
			// 路径中只需要判断一个路径， 返回 ClassPathResource/FileUrlResource/UrlResource/ClassPathContextResource 中的某一个
			return new Resource[] {getResourceLoader().getResource(locationPattern)};
		}
	}
}


@Override
public boolean isPattern(String path) {
    return (path.indexOf('*') != -1 || path.indexOf('?') != -1);
}


// 调用到 DefaultResourceLoader 的 getResource 方法

public Resource getResource(String location) {

	Assert.notNull(location, "Location must not be null");

	// 如果有定义的解析协议的话，调用自定义协议进行解析
	for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
		Resource resource = protocolResolver.resolve(location, this);
		if (resource != null) {
			return resource;
		}
	}

	// 以 / 开头
	if (location.startsWith("/")) {
		return getResourceByPath(location);
	}
	// 以 classpath: 开头, 返回
	else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
		return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
	}
	else {
		try {
			// Try to parse the location as a URL...
			URL url = new URL(location);
			return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
		}
		catch (MalformedURLException ex) {
			// No URL -> resolve as resource path.
			return getResourceByPath(location);
		}
	}

}


```


第二步 获取需要的 ConfigurableListableBeanFactory 