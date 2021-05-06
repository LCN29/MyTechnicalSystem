package com.lcn29.spring2.bean;

import lombok.Data;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 17:40
 */
@Data
public class RuntimeBeanReference implements BeanReference {

    private final String beanName;

    private final boolean toParent;

    private Object source;

    public RuntimeBeanReference(String beanName) {
        this(beanName, false);
    }

    public RuntimeBeanReference(String beanName, boolean toParent) {
        this.beanName = beanName;
        this.toParent = toParent;
    }
}
