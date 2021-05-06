package com.lcn29.spring2.reader;


import com.lcn29.spring2.bean.NamespaceHandlerResolver;
import com.lcn29.spring2.exception.BeanDefinitionStoreException;
import com.lcn29.spring2.registry.BeanDefinitionRegistry;
import com.lcn29.spring2.resource.EncodedResource;
import com.lcn29.spring2.resource.Resource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * <pre>
 *  从 Xml 中读取 BeanDefinition
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:28
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    private SourceExtractor sourceExtractor = new NullSourceExtractor();

    private NamespaceHandlerResolver namespaceHandlerResolver;

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public int loadBeanDefinitions(Resource resource) throws Exception {
        return loadBeanDefinitions(new EncodedResource(resource));
    }

    public int loadBeanDefinitions(EncodedResource encodedResource) throws Exception {

        // TODO 在 Spring 的实现中, 这里会将当前正在解析的 EncodeResource 放在当前线程的 ThreadLocal
        try {
            InputStream inputStream = encodedResource.getResource().getInputStream();
            try {
                InputSource inputSource = new InputSource(inputStream);
                if (encodedResource.getEncoding() != null) {
                    inputSource.setEncoding(encodedResource.getEncoding());
                }
                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
            } finally {
                inputStream.close();
            }
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("IOException parsing XML document from " + encodedResource.getResource(), ex);
        }
    }

    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource) {
        try {
            Document doc = doLoadDocument(inputSource);
            int count = registerBeanDefinitions(doc, resource);
            return count;
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * 获取 Document
     * <p>
     * 在 Spring 中为了获取 Document 做了大量的封装, 此处就直接按照 Sax 获取了
     * 在 Spring 中涉及到的类有 DocumentLoader, DefaultDocumentLoader, EntityResolver, ErrorHandler 等
     *
     * @param inputSource Sax 的输入源格式
     * @return
     * @throws Exception
     */
    private Document doLoadDocument(InputSource inputSource) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 不使用命名空间
        factory.setNamespaceAware(false);
        // 不启用校验
        factory.setValidating(false);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputSource);
        return doc;
    }

    /**
     * 解析 Document 和 注册 BeanDefinition
     *
     * @param doc
     * @param resource
     * @return
     * @throws BeanDefinitionStoreException
     */
    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
        BeanDefinitionDocumentReader documentReader = new DefaultBeanDefinitionDocumentReader();
        int countBefore = getRegistry().getBeanDefinitionCount();
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }

    public XmlReaderContext createReaderContext(Resource resource) {
        return new XmlReaderContext(resource, sourceExtractor, this, getNamespaceHandlerResolver());
    }

    public NamespaceHandlerResolver getNamespaceHandlerResolver() {
        if (this.namespaceHandlerResolver == null) {
            this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
        }
        return this.namespaceHandlerResolver;
    }

    protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
        ClassLoader cl = (getResourceLoader() != null ? getResourceLoader().getClassLoader() : getBeanClassLoader());
        return new DefaultNamespaceHandlerResolver(cl);
    }

}
