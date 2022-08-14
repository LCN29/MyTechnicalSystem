# PostProcessor

![Alt 'PostProcessor'](https://github.com/PictureRespository/Java/blob/main/Spring/PostProcessor/PostProcessorUML.png?raw=true)

## 1 BeanFactoryPostProcessor

```java
public interface BeanFactoryPostProcessor {

  /**
   * BeanFactory 已经初始完成, 可以对其进行修改
   * 而且这时候全部的 bean definition 已经加载完成, 但是没有进行实例化, 可以对这些 bean definition 添加, 重载属性等操作
   */
  void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
```

## 2 BeanDefinitionRegistryPostProcessor

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

  /**
   * 通过代码的形式，动态地向容器添加 BeanDefinition
   */
  void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}
```


## 3 BeanPostProcessor

### 3.1 BeanPostProcessor

对象初始前后的的回调

```java
public interface BeanPostProcessor {

  /**
   *  在 bean 属性填充完成后, 进行初始化前, 调用初始化方法 (实现了 InitializingBean, 调用 afterPropertiesSet 方法和自定义 init 方法) 之前调用
   *  执行规则: 遍历所有的 BeanPostProcessor, 执行其 postProcessBeforeInitialization， 执行到第一个返回 null 时, 返回上一次的执行结果
   */
  default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

  /**
   *  在 bean 属性填充完成后, 进行初始化中后, 调用初始化方法 (实现了 InitializingBean, 调用 afterPropertiesSet 方法和自定义 init 方法) 之后调用
   *  执行规则: 遍历所有的 BeanPostProcessor, 执行其 postProcessBeforeInitialization， 执行到第一个返回 null 时, 返回上一次的执行结果
   */
  default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}


```



### 3.2 InstantiationAwareBeanPostProcessor

对象实例化前后的回调 和 实例化后设置 PropertyValues 的回调

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

  /**
   *  在对象实例化前直接返回一个对象（如代理对象）来代替通过内置的实例化流程创建对象，
   *  一旦这个方法返回了一个非 null 的对象, Spring 默认的 bean 创建流程将不会被执行, 
   *  由这个方法创建的对象, 进一步处理的只有 BeanPostProcessor 的 postProcessAfterInitialization 方法
   *  执行规则: 遍历所有的 InstantiationAwareBeanPostProcessor, 执行其 postProcessBeforeInitialization， 执行到第一个返回 null 时, 返回上一次的执行结果
   */
  default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

  /**
   *  在对象实例化后, 对对象进行属性填充前调用
   *  执行规则: 遍历所有的 InstantiationAwareBeanPostProcessor, 执行其 postProcessAfterInstantiation, 执行到第一个返回 false 时, 就结束, 这时不会进行属性赋值， 全部返回 true, 就会进入属性赋值
   *
   */
  default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}


  /**
   * 在对象实例化后, 解析完对象的属性和属性的值, 对对象的进行真正的赋值之前执行，可以用来检查和修改属性，最终返回的PropertyValues会应用到bean中, @Autowired、@Resource等就是根据这个回调来实现最终注入依赖的属性的
   * 执行规则: 遍历所有的 InstantiationAwareBeanPostProcessor 的 postProcessProperties
   *
   */
  default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		return null;
	}

  /**
   * 在 5.1 版本标注为过期了, 建议用 postProcessProperties 替代
   */
  @Deprecated
  default PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
		return pvs;
	}

}
```


### 3.3 DestructionAwareBeanPostProcessor

```java

public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

  /**
   * 销毁逻辑
   * 执行规则: requiresDestruction 判断为 true, 就会执行到
   *
   */
  void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

  /**
   * 这个 bean 实例化, 属性填充完成，初始化完成后, 判断这个 DestructionAwareBeanPostProcessor 的是否使用这个 bean, 使用的话就给这个 bean 的销毁逻辑里面添加上 postProcessBeforeDestruction 的逻辑
   * 执行规则:  每个 bean 创建完成后, 都会判断
   *
   */
  default boolean requiresDestruction(Object bean) {
		return true;
	}

}

```

### 3.4 MergedBeanDefinitionPostProcessor

这个接口主要是 spring 框架内部来使用, 用于对 BeanDefinition 进行处理

```java
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

  /**
    * 在 bean 实例化后, 填充属性前调用， 对给定的 beanDefinition 进行进一步处理, 可以用来修改 properties, 添加 meta 信息等
    * 执行规则:  每个 MergedBeanDefinitionPostProcessor 的 postProcessMergedBeanDefinition 都会执行一次, 但是 RootBeanDefinition 身上有一个 boolean postProcessed 的属性, 处理过一次后，会变为 true, 也就是每个
    * RootBeanDefinition 只会执行一次
    *
    */
  void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

  /**
    *
    * 对给定的 beanName 对应的 bean definition 的进行重置, 可以用于清除各种 metadata 缓存等
    * 执行规则:  每个 MergedBeanDefinitionPostProcessor 的 resetBeanDefinition 都会执行一次
    */
  default void resetBeanDefinition(String beanName) {
  }
}
```


### 3.5 SmartInstantiationAwareBeanPostProcessor

这个接口主要是 spring 框架内部来使用

```java

public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

  /**
   * 用来返回目标对象的类型(比如代理对象通过raw class获取proxy type 用于类型匹配)
   * 执行规则: 每一个 SmartInstantiationAwareBeanPostProcessor 的 predictBeanType 都会执行一下, 直到获取到 Class, 同时 class 不是 FactoryBean 的子类, 就终止
   *
   */
  default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

  /**
   * 用来解析获取用来实例化的构造器 
   * 执行规则:  每一个 SmartInstantiationAwareBeanPostProcessor 的 predictBeanType 都会执行一下, 直到第一个获取到 Constructor 不会 null
   */
  default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)	throws BeansException {
		return null;
	}

  /**
   * 获取要提前暴露的 bean 的引用, 用来支持单例对象的循环引用
   * 执行规则: 每一个 SmartInstantiationAwareBeanPostProcessor 的 getEarlyBeanReference 都会执行一下
   * 
   */
  default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}

```