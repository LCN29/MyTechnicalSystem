package com.lcn29.spring2.reader;

import java.util.LinkedList;
import java.util.List;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 17:19
 */
public class ReplaceOverride extends MethodOverride{

    private final String methodReplacerBeanName;

    private final List<String> typeIdentifiers = new LinkedList<>();

    public ReplaceOverride(String methodName, String methodReplacerBeanName) {
        super(methodName);
        this.methodReplacerBeanName = methodReplacerBeanName;
    }

    public void addTypeIdentifier(String identifier) {
        this.typeIdentifiers.add(identifier);
    }

}
