package com.lcn29.spring2.bean;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 15:29
 */
public class ManagedArray extends ManagedList<Object> {

    volatile Class<?> resolvedElementType;

    public ManagedArray(String elementTypeName, int size) {
        super(size);
        setElementTypeName(elementTypeName);
    }
}
