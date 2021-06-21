
## 01. 循环依赖解决

Spring 中循环依赖可以区分为 3 种

1. 构造函数的循环依赖
2. 单例 bean 的属性循环依赖
3. 非单例 bean 的属性循环依赖

Spring 的循环依赖只能解决第 2 种情况, 其他 2 种的话, 无法解决。


单例 bean 的属性循环依赖的解决关键: 三级缓存

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

    /**
     * 一级缓存: 用于存放完全初始化好的 bean
     * key: beanName value: bean
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    /**
     * 二级缓存: 存放原始的 bean 对象(尚未填充属性), 用于解决循环依赖
     * key: beanName  value: 为填充属性的 bean
     */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    /**
     * 三级缓存: 存放 bean 工厂对象，用于解决循环依赖
     * key： beanName, value: 可以获取到当前 bean 对象的 ObjectFactory 函数
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

    /**
     * 正在创建的 beanName 集合
     */
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    /**
     * 已经注册的 beanName
     */
    private final Set<String> registeredSingletons = new LinkedHashSet<>(256);
}
```

涉及到 Bean 的实例化的 4 个关键方法

> 1. getSingleton
> 2. doCreateBean
> 3. populateBean
> 4. addSingleton


假设现在有

```java
public class A {
    private B b;
}

public class B {
    private A a;
}
```


创建 class A 的 实例

> 1. 通过 getSingleton 去各级缓存中查找, 获取不到
> 2. A 实例未创建, 调用 doCreateBean 创建 A 的实例, 入参有 ObjectFactory, 这时 A 还未创建, 调用 ObjectFactory.getObject 方法获取实例, getObject实际是调用 createBean 进行 bean 的创建
> 3. 要创建的 bean 为单例, 入参的允许自动解决循环依赖为 true, 同时正在创建的 bean name 集合包含当前的 beanName, 向三级缓存中添加返回这个还没初始化的 bean, key 为 beanName, value 为 ObjectFactory, 实现逻辑为做了一点处理后, 返回 bean
> 4. 经过二, 三步得到了实例 A, 但是这时候 A 的属性还未初始化, 调用 populateBean 进行属性的填充, 通过 getSingleton 获取 B 的实例, 在各个缓存中获取不到, 开始创建类 B 的实例
> 5. 进行二, 三步, 这时得到了实例 B, 同样调用 populateBean 进行属性的填充
> 6. 通过 getSingleton 获取 A 的实例时, 这次在第三层缓存中获取到了能得到 A 的 ObjectFactory 函数, 调用其 getObject 得到了A 的实例, 这时候 A 还未初始化, 把获取到的 A 添加到二级缓存, 从三级缓存中移除 A 
> 7. 调用 addSingletion 把实例 B 放到一级缓存, 从二三级缓存中删除 B

> 8. 这里又回到实例 A 的 populateBean 方法, 这时候获取到 B 的实例了, A 初始化完成, 调用 addSingletion 把实例 A 放到一级缓存, 从二三级缓存中删除 A



```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {

    // 一级缓存获取
    Object singletonObject = this.singletonObjects.get(beanName);

    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {

        // 二级缓存获取
        singletonObject = this.earlySingletonObjects.get(beanName);

        if (singletonObject == null && allowEarlyReference) {

            // 对一级缓存加锁
            synchronized (this.singletonObjects) {
                // 再次检查

                // 从一级缓存获取
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    
                    // 从二级缓存获取
                    singletonObject = this.earlySingletonObjects.get(beanName);

                    if (singletonObject == null) {
                        // 从三级缓存获取
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            // 调用 ObjectFactory 获取对象
                            singletonObject = singletonFactory.getObject();
                            // 添加到二级缓存
							this.earlySingletonObjects.put(beanName, singletonObject);
							// 移除三级缓存
                            this.singletonFactories.remove(beanName);
                        }

                    }
                }

            }
        }

    }

    return singletonObject;
}
```


## 02. 三级缓存

这实际上涉及到 AOP，如果创建的 Bean 是有代理的，那么注入的就应该是代理 Bean，而不是原始的 Bean。但是 Spring 一开始并不知道 Bean 是否会有循环依赖。
通常情况下（没有循环依赖的情况下），Spring 都会在完成填充属性，并且执行完初始化方法之后再为其创建代理。
但是，如果出现了循环依赖的话，Spring 就不得不为其提前创建代理对象，否则注入的就是一个原始对象，而不是代理对象。

因此，这里就涉及到应该在哪里提前创建代理对象。 ObjectFactory.getObject 实际调用的是下面的方法

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                // 如果需要代理，这里会返回代理对象；否则返回原始对象
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
}
```

在 Spring 中 SmartInstantiationAwareBeanPostProcessor 只有 2 个实现类
>1. InstantiationAwareBeanPostProcessorAdapter: 一个适配器, 实现了 SmartInstantiationAwareBeanPostProcessor 所有方法, 但是返回的都是默认值, 没有任何实现
>2. AbstractAutoProxyCreator

```java

public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        // 记录已被代理的对象, 放入 2 层缓存
        this.earlyProxyReferences.put(cacheKey, bean);
        return wrapIfNecessary(bean, beanName, cacheKey);
    }
}
```

其实完全可以放弃第三层缓存, 将 

```java
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized (this.singletonObjects) {
        // 判断一级缓存中不存在此对象
        if (!this.singletonObjects.containsKey(beanName)) { 
            // 直接从工厂中获取 Bean
            object o = singletonFactory.getObject(); 
            // 添加至二级缓存中
            this.earlySingletonObjects.put(beanName, o); 
            // 已经创建的 beanName 集合
            this.registeredSingletons.add(beanName);
        }
    }
}
```

这样的话，每次实例化完 Bean 之后就直接去创建代理对象，并添加到二级缓存中, 功能也是正常的。

但是这样会导致实例的代理对象的创建时间提前:
在三级缓存下：一般都是 bean 创建完成, 然后 bean 对象初始化后, 最后才进行代理。   
而在二级缓存下, 这是 bean 创建完成, 进行代理, bean 初始化。

但是这样违背了 Spring 设计原则: 在 Bean 初始化完成之后才为其创建代理

## 3 参考
[Spring 解决循环依赖必须要三级缓存吗？](https://juejin.cn/post/6882266649509298189)