package com.lcn29.spring2.bean;


import com.lcn29.spring2.reader.MutablePropertyValues;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:40
 */
public interface BeanDefinition extends BeanMetadataElement {

    /**
     * 获取 bean 的 class 名
     *
     * @return
     */
    String getBeanClassName();

    /**
     * 返回这个 beanDefinition 的 FactoryBean 名, 如果有的话
     *
     * @return
     */
    String getFactoryBeanName();

    String getParentName();

    void setParentName(String parentName);

    MutablePropertyValues getPropertyValues();

    void setScope(String scope);

    String getScope();

    boolean isAbstract();

    ConstructorArgumentValues getConstructorArgumentValues();

    default boolean hasConstructorArgumentValues() {
        return !getConstructorArgumentValues().isEmpty();
    }

    void setLazyInit(boolean lazyInit);

    void setBeanClassName(String beanClassName);
}
