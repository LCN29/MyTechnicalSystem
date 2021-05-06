package com.lcn29.spring2.bean;

import com.lcn29.spring2.registry.BeanDefinitionRegistry;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 16:11
 */
public interface BeanNameGenerator {

    String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry);
}
