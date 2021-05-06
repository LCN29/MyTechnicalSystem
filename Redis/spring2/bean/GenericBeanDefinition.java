package com.lcn29.spring2.bean;

import lombok.Data;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 11:10
 */
@Data
public class GenericBeanDefinition extends AbstractBeanDefinition {

    private String parentName;

    public GenericBeanDefinition() {
        super();
    }
}
