# Enviroment 

## 1. PropertyResolver

PropertyResolver 属性解析器

```java

public interface PropertyResolver {
    /** 是否包含某个属性 */
    boolean containsProperty(String key);

    /** 获取某个属性，可能为空 */
    String getProperty(String key);

    /** 带默认值的属性获取 */
    String getProperty(String key, String defaultValue);

    /** 获取指定类型的某个属性,  */
    <T> T getProperty(String key, Class<T> targetType);

    /** 带默认值的获取指定类型属性 */
    <T> T getProperty(String key, Class<T> targetType, T defaultValue);

    /** 获取属性, 获取不到抛出异常 */
    String getRequiredProperty(String key) throws IllegalStateException;

    /** 获取属性，并转换为指定的类型，获取不到抛出异常 */
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

    /** 将指定文本中 ${...} 替换成正常的属性, 如果没法获取对应的属性进行替换, 忽略 */
    String resolvePlaceholders(String text);

    /** 将指定文本中 ${...} 替换成正常的属性, 如果没法获取对应的属性进行替换, 抛出异常 */
    String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;
}
```

从接口的定义可以看出 PropertyResolver 定义的作用有
1. 判断某个属性是否存在
2. 通过 key 获取对应的 value
3. 将字符串中的 ${...} 中的 key 替换成对应的 value



## 3. PropertySource

PropertyResolver 属性解析器, 那么他的属性源一般都是 `PropertySource`, PropertySource 主要用于存储一个 key 和 key 对应的一个 value, PropertySource 的定义如下

```java
public abstract class PropertySource<T> {

    
    protected final String name;

	protected final T source;

	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

    /** 通过 name 获取属性 */
    public abstract Object getProperty(String name);
}
```

可以从类的定义可以知道, PropertySource 的 key 对应的只有一个 value。但是我们的 value 可以是一个 Map 之类的. 达到存储多个 value 的情况

PropertySource 两个特殊的子类：StubPropertySource 用于占位用, 通过 key 获取的 value 都是 null, ComparisonPropertySource 用于集合排序，不允许获取属性值, 获取 name 的方法都会抛出异常



## 2. Enviroment

从接口的定义中可以看出，Enviroment 也是一个 PropertyResolver, 和 PropertyResolver 的区别在于, Enviroment 多了 Profiles(剖面)的概念, 只有激活的剖面的组件/配置才会注册到 Spring 容器，类似于 maven 中 profile
