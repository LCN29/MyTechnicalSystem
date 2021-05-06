package com.lcn29.spring2.reader;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 11:27
 */
public interface AttributeAccessor {

    void setAttribute(String name, Object value);

    Object getAttribute(String name);

    Object removeAttribute(String name);

    boolean hasAttribute(String name);

    String[] attributeNames();
}
