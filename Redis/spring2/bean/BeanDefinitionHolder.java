package com.lcn29.spring2.bean;


import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *  BeanDefinition 的句柄, 多一层包装
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:41
 */

@Getter
@Setter
public class BeanDefinitionHolder implements BeanMetadataElement {

    private final BeanDefinition beanDefinition;

    private final String beanName;

    private final String[] aliases;

    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
        this(beanDefinition, beanName, null);
    }

    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, String[] aliases) {
        this.beanDefinition = beanDefinition;
        this.beanName = beanName;
        this.aliases = aliases;
    }

    @Override
    public Object getSource() {
        return this.beanDefinition.getSource();
    }
}
