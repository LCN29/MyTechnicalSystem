# SpringBean 生命周期

## 1.1 通过 beanName 获取 BeanDefinition

通过 beanName 获取到这个 bean 的 bean 定义 BeanDefinition (实际实现: RootBeanDefinition)

在使用中, spring 支持通过 abstract 属性设置一个 bean 为抽象的 bean 定义, 然后子类通过 parent 属性继承这个父类的属性,
当父子出现相同的属性名时, 以子类的为主

```xml

<beans>
    <bean id="txProxyTemplate" abstract="true" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean">
    　　<property name="transactionManager" ref="transactionManager"/>
    　　<property name="transactionAttributes">
    　　　　<props>
    　　　　<prop key="*">PROPAGATION_REQUIRED</prop>
    　　　　</props>
    　　</property>
    </bean>


    <bean id="myService" parent="txProxyTemplate">
    　　<property name="target">
    　　　　<bean class="org.springframework.samples.MyServiceImpl">
    　　　　</bean>
    　　</property>
    </bean>  
    
</beans>
```

通过 getMergedLocalBeanDefinition(String beanName) 获取到这个 beanName 最终的 BeanDefinition

```java
protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd, BeanDefinition containingBd) throws BeanDefinitionStoreException {
    
    // 锁住 Map<String, RootBeanDefinition> mergedBeanDefinitions beanName 对应的最终 beanDefinition 缓存
    // 参数 3 一般为空, containingBd 为空, 再次从缓存中获取这个 beanName 对应的 RootBeanDefinition
    // 获取到的 RootBeanDefinition 还是为空,

    // bd 的 parentName 为空  RootBeanDefinition mbd = bd instanceof RootBeanDefinition ? ((RootBeanDefinition) bd).cloneBeanDefinition() : new RootBeanDefinition(bd);
    // bd 的 parentName 不为空, 通过 parentName 获取到最终的 beanName, 
    // beanName 等于真正的 parentName 
    // - 等于, 判断当前的 beanFactory 的父级  beanFactory, 父级的 beanFactory 为空或者不是 ConfigurableBeanFactory, 抛出异常， 通过父级的 beanFactory 的 getMergedBeanDefinition(真正的 parentName), 获取到 parentName 的 BeanDefinition
    // - 不等于, 重新调用当前的  getMergedBeanDefinition(真正的 parentName)
    // 将通过 parentName 获取到的 BeanDefinition, 作为 RootBeanDefinition 的构造函数的参数, 创建一个新的 RootBeanDefinition， 调用这个新的 RootBeanDefinition 的 overrideFrom 方法, 把 bd 的属性拷贝过来

    // 最终的 RootBeanDefinition mbd 的 Scope 属性为空的话，RootBeanDefinition mbd 默认为 singleton
    // containingBd 不为空, containingBd 的不是单例, mbd 不是单例, 设置 mbd 的 Scope 等于 containingBd 的 Scope
    // containingBd 为 空, 并且 boolean cacheBeanMetadata = true, 将这个最终的 RootBeanDefinition 存入到 Map<String, RootBeanDefinition> mergedBeanDefinitions
}
```

## 1.2 FactoryBean 处理

当前的 RootBeanDefinition bd 不是 Abstract 的, 同时是单例的, 而且不需要懒加载(!bd.isLazyInit())
- 当前的 RootBeanDefinition 对应的 bean 实现了 FactoryBean 的子类
- 通过 & + beanName 从容器中获取这个 bean, 获取到的 bean 不是 FactoryBean, 跳出这个 if
- 如果当前的 bean 是 SmartFactoryBean 的子类，强制后, 调用其 isEagerInit() 方法, 得到是否需要立即加载
- FactoryBean 对应的真正的 bean 默认不是立即加载的，调用 isEagerInit() 方法, 得到的为 true, 立即加载 bean

- 当前的 RootBeanDefinition 对应的 bean 没有实现了 FactoryBean 的子类





