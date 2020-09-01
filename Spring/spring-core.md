# spring-core


// 环境的初始化
// 在 refere 中进行 bean 的初始化

new ClassPathXmlApplicationContext("classpath:spring-bean.xml") 流程


1. 
```java

public AbstractApplicationContext() {
	this.resourcePatternResolver = getResourcePatternResolver();
}
```


2. 
```java
@Override
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

```java
protected String resolvePath(String path) {
    return getEnvironment().resolveRequiredPlaceholders(path);
}

```