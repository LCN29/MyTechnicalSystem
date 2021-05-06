package com.lcn29.spring2.reader;

import com.lcn29.spring2.registry.BeanDefinitionRegistry;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:27
 */
public interface BeanDefinitionReader {

    /**
     * 获取当前的 BeanDefinition 容器
     * @return
     */
    BeanDefinitionRegistry getRegistry();

    ClassLoader getBeanClassLoader();

    ResourceLoader getResourceLoader();
}
