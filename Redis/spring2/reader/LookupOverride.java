package com.lcn29.spring2.reader;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 17:09
 */
@Data
public class LookupOverride extends MethodOverride {

    private final String beanName;

    private Method method;

    public LookupOverride(String methodName, String beanName) {
        super(methodName);
        this.beanName = beanName;
    }

}
