# PropertySource


在 Spring 中, 在使用 xml 文件时，在我们指定文件路径的时候，使用 ${变量} 的形式，动态设置我们的配置文件
```java

String xmlPath = "/Users/${user.name}/config.xml";

// xmlPath 里面的${user.name} 会被替换成具体的值
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(xmlPath);
```

将变量 ${user.name} 变为具体的值，Spring 内部做了
>1. 当前电脑的系统变量和环境变量加载到程序，保存起来
>2. 从加载到的变量中查询 我们变量 对应的值，将对应的值替换掉我们的变量

## 1. PropertySource

在上面的第一步中，我们加载到的变量就是通过 PropertySource 进行存储的。  
PropertySource 主要用于存储一个 key 和 key 对应的一个 value, PropertySource 的定义如下

```java
public abstract class PropertySource<T> {
    /** 属性名 */
    protected final String name;
    /** 属性值 */
	protected final T source;

    /** 是否包含某属性 */
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

    /** 通过 name 获取属性 */
    public abstract Object getProperty(String name);
}
```

## 2. PropertySource 的实现