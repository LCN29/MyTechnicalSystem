package com.lcn29.spring2.bean;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 16:57
 */
public interface NamespaceHandler {

    void init();

    BeanDefinition parse(Element element, ParserContext parserContext);

    BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);

}
