package com.lcn29.spring2.bean;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 16:59
 */
public interface NamespaceHandlerResolver {

    NamespaceHandler resolve(String namespaceUri);
}
