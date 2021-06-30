# PostProcessor

BeanPostProcessor

```java
public interface BeanPostProcessor {

    default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

    default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```




InstantiationAwareBeanPostProcessor

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * 可以在 bean 实例化之前, 返回一个代理的 bean 来代替实际需要的 bean
     * 如果返回的是一个非空的对象, 那么 bean 的创建的过程就会终止, 唯一可以进一步处理的只有 BeanPostProcessor 的 postProcessAfterInitialization 方法
     *
     */
    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

    /**
     * 这是在给定的 bean 实例上执行自定义字段注入的理想回调, 在 Spring 属性自动填充
     *
     */
    default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

    /**
     * 在对实例化的 bean 进行属性填充的时候, 调用
     * 1. 通过注解注入属性的
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


```java
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

    /**
     * 对指定 bean 的合并 bean definition 进行后处理
     *
     */
    void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

    /**
     *
     * 对给定的 beanName 对应的 bean definition 的所有元数据
     *
     */
    default void resetBeanDefinition(String beanName) {
	  }
}
```

```java
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

  void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

  /**
   * 判断 bean 是否需要执行上面的 postProcessBeforeDestruction
   */
  default boolean requiresDestruction(Object bean) {
		return true;
	}

}
```