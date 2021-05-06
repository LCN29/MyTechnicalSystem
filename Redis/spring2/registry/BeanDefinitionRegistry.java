package com.lcn29.spring2.registry;

import com.lcn29.spring2.bean.BeanDefinition;
import com.lcn29.spring2.exception.BeanDefinitionStoreException;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:39
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

    /**
     * 向 容器注册 BeanDefinition
     *
     * @param beanName       bean 的名字
     * @param beanDefinition bean 定义
     * @throws BeanDefinitionStoreException
     */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionStoreException;

    /**
     * 当前的容器是否包含了这个名字的 beanDefinition
     *
     * @param beanName bean 的名字
     * @return 是否已经存在
     */
    boolean containsBeanDefinition(String beanName);

    /**
     * 当前容器里面已经有多少个 BeanDefinition
     *
     * @return
     */
    int getBeanDefinitionCount();

    boolean isBeanNameInUse(String beanName);
}
