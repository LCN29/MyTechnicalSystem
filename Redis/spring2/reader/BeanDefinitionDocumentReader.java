package com.lcn29.spring2.reader;

import com.lcn29.spring2.exception.BeanDefinitionStoreException;
import org.w3c.dom.Document;

/**
 * <pre>
 * 从 Document 读取 BeanDefinition
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:29
 */
public interface BeanDefinitionDocumentReader {

    /**
     * 开始 Document 的读取和 BeanDefinition 的注册
     *
     * @param doc
     * @param readerContext
     * @throws BeanDefinitionStoreException
     */
    void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) throws BeanDefinitionStoreException;
}
