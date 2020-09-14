# 从配置文件中加载 bean

```java
public ClassPathXmlApplicationContext (String[] configLocations, boolean refresh, ApplicationContext parent) {

	// 第一步
	super(parent);

	// 第二步
	setConfigLocations(configLocations);

	// 默认为 true
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

同时在解析的过程中，会进行 ConfigurableEnvironment 的初始，通过调用 AbstractApplicationContext 的 createEnvironment 方法(protected 方法), 创建出当前的环境, 默认为 StandardEnvironment

## 


## 


## 


