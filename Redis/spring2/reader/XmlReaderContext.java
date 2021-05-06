package com.lcn29.spring2.reader;

import com.lcn29.spring2.bean.BeanDefinition;
import com.lcn29.spring2.bean.NamespaceHandlerResolver;
import com.lcn29.spring2.registry.BeanDefinitionRegistry;
import com.lcn29.spring2.resource.Resource;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-28 11:16
 */
public class XmlReaderContext extends ReaderContext {

    private final XmlBeanDefinitionReader reader;

    private final NamespaceHandlerResolver namespaceHandlerResolver;

    public XmlReaderContext(Resource resource, SourceExtractor sourceExtractor, XmlBeanDefinitionReader reader,  NamespaceHandlerResolver namespaceHandlerResolver) {
        super(resource, sourceExtractor);
        this.reader = reader;
        this.namespaceHandlerResolver = namespaceHandlerResolver;
    }

    public final BeanDefinitionRegistry getRegistry() {
        return this.reader.getRegistry();
    }

    public final ClassLoader getBeanClassLoader() {
        return this.reader.getBeanClassLoader();
    }

    public String generateBeanName(BeanDefinition beanDefinition) {
        return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
    }

    public final NamespaceHandlerResolver getNamespaceHandlerResolver() {
        return this.namespaceHandlerResolver;
    }
}
