package com.lcn29.spring2.bean;

import com.lcn29.spring2.registry.BeanDefinitionRegistry;
import com.lcn29.spring2.util.BeanDefinitionReaderUtils;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 16:11
 */
public class DefaultBeanNameGenerator implements BeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        return BeanDefinitionReaderUtils.generateBeanName(definition, registry);
    }
}
