package com.lcn29.spring2.reader;

import com.lcn29.spring2.bean.BeanDefinitionHolder;
import com.lcn29.spring2.exception.BeanDefinitionStoreException;
import com.lcn29.spring2.util.BeanDefinitionReaderUtils;
import com.lcn29.spring2.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-28 10:11
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

    private XmlReaderContext readerContext;

    private BeanDefinitionParserDelegate delegate;

    @Override
    public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) throws BeanDefinitionStoreException {
        this.readerContext = readerContext;
        Element root = doc.getDocumentElement();
        this.doRegisterBeanDefinitions(root);
    }

    protected void doRegisterBeanDefinitions(Element root) {
        BeanDefinitionParserDelegate parent = this.delegate;
        this.delegate = createDelegate(getReaderContext(), root, parent);

        if (this.delegate.isDefaultNamespace(root)) {

            String profileSpec = root.getAttribute("profile");
            if (StringUtils.hasText(profileSpec)) {
                if (StringUtils.hasText(profileSpec)) {
                    String[] specifiedProfiles = StringUtils.tokenizeToStringArray(profileSpec, ",; ");
                    // TODO 当前配置的环境是否支持上面的 profile, 不支持直接返回
                }
            }
        }

        // 提供子类进行自定义, 在 xml 正式解析前进行预处理
        preProcessXml(root);
        parseBeanDefinitions(root, this.delegate);
        // 提供子类进行自定义, 在 xml 正式解析后进行处理
        postProcessXml(root);

        this.delegate = parent;
    }

    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {
        BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    protected final XmlReaderContext getReaderContext() {
        return this.readerContext;
    }

    protected void preProcessXml(Element root) {
    }

    protected void postProcessXml(Element root) {
    }

    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {

        if (delegate.isDefaultNamespace(root)) {
            NodeList nl = root.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element ele = (Element) node;
                    if (delegate.isDefaultNamespace(ele)) {
                        parseDefaultElement(ele, delegate);
                    } else {
                        // 自定义标签的处理
                        //delegate.parseCustomElement(ele);
                    }
                }
            }
        } else {
            // 自定义标签的处理
            //delegate.parseCustomElement(ele);
        }

    }


    private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
        if (delegate.nodeNameEquals(ele, "import")) {
            importBeanDefinitionResource(ele);
        } else if (delegate.nodeNameEquals(ele, "alias")) {
            processAliasRegistration(ele);
        } else if (delegate.nodeNameEquals(ele, "bean")) {
            processBeanDefinition(ele, delegate);
        } else if (delegate.nodeNameEquals(ele, "beans")) {
            // recurse
            doRegisterBeanDefinitions(ele);
        }
    }

    protected void importBeanDefinitionResource(Element ele) {
    }

    protected void processAliasRegistration(Element ele) {
        String name = ele.getAttribute("name");
        String alias = ele.getAttribute("alias");
        boolean valid = true;

        if (!StringUtils.hasText(name)) {
            valid = false;
        }

        if (!StringUtils.hasText(alias)) {
            valid = false;
        }

        if (valid) {
            try {
                // 添加别名到容器中
                getReaderContext().getRegistry().registerAlias(name, alias);
            } catch (Exception ex) {
                // 注册异常事件
            }
            // 注册事件
        }
    }

    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {

        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
        if (bdHolder != null) {
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
            try {
                // Register the final decorated instance.
                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
            } catch (BeanDefinitionStoreException ex) {
                // 异常解析处理
            }
            // 注册 BeanComponentDefinition 事件
        }
    }

}
