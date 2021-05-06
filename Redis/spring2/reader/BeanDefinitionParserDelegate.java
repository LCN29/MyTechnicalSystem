package com.lcn29.spring2.reader;

import com.lcn29.spring2.bean.*;
import com.lcn29.spring2.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-28 14:26
 */
public class BeanDefinitionParserDelegate {

    public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

    private final XmlReaderContext readerContext;

    private final Set<String> usedNames = new HashSet<>();

    private final DocumentDefaultsDefinition defaults = new DocumentDefaultsDefinition();

    private final ParseState parseState = new ParseState();

    public BeanDefinitionParserDelegate(XmlReaderContext readerContext) {
        this.readerContext = readerContext;
    }

    public void initDefaults(Element root, BeanDefinitionParserDelegate parent) {

        // 默认配置的设置
        populateDefaults(this.defaults, (parent != null ? parent.defaults : null), root);
        // 注册事件
        //this.readerContext.fireDefaultsRegistered(this.defaults);
    }

    protected void populateDefaults(DocumentDefaultsDefinition defaults, DocumentDefaultsDefinition parentDefaults, Element root) {
        String lazyInit = root.getAttribute("default-lazy-init");

        if (isDefaultValue(lazyInit)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
            lazyInit = (parentDefaults != null ? parentDefaults.getLazyInit() : "false");
        }
        defaults.setLazyInit(lazyInit);

        String merge = root.getAttribute("default-merge");
        if (isDefaultValue(merge)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
            merge = (parentDefaults != null ? parentDefaults.getMerge() : "false");
        }
        defaults.setMerge(merge);

        String autowire = root.getAttribute("default-autowire");
        if (isDefaultValue(autowire)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to 'no'.
            autowire = (parentDefaults != null ? parentDefaults.getAutowire() : "no");
        }
        defaults.setAutowire(autowire);

        if (root.hasAttribute("default-autowire-candidates")) {
            defaults.setAutowireCandidates(root.getAttribute("default-autowire-candidates"));
        } else if (parentDefaults != null) {
            defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
        }

        if (root.hasAttribute("default-init-method")) {
            defaults.setInitMethod(root.getAttribute("default-init-method"));
        } else if (parentDefaults != null) {
            defaults.setInitMethod(parentDefaults.getInitMethod());
        }

        if (root.hasAttribute("default-destroy-method")) {
            defaults.setDestroyMethod(root.getAttribute("default-destroy-method"));
        } else if (parentDefaults != null) {
            defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
        }

        defaults.setSource(this.readerContext.extractSource(root));

    }

    private boolean isDefaultValue(String value) {
        return ("default".equals(value) || "".equals(value));
    }

    public boolean isDefaultNamespace(Node node) {
        return isDefaultNamespace(getNamespaceURI(node));
    }

    public boolean isDefaultNamespace(String namespaceUri) {
        return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
    }

    public String getNamespaceURI(Node node) {
        return node.getNamespaceURI();
    }

    public boolean nodeNameEquals(Node node, String desiredName) {
        return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
    }

    public String getLocalName(Node node) {
        return node.getLocalName();
    }

    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
        return parseBeanDefinitionElement(ele, null);
    }

    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, BeanDefinition containingBean) {
        String id = ele.getAttribute("id");
        String nameAttr = ele.getAttribute("name");

        List<String> aliases = new ArrayList<>();
        if (StringUtils.hasLength(nameAttr)) {
            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, ",; ");
            aliases.addAll(Arrays.asList(nameArr));
        }

        String beanName = id;
        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
            beanName = aliases.remove(0);
        }

        if (containingBean == null) {
            checkNameUniqueness(beanName, aliases, ele);
        }

        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);

        if (beanDefinition != null) {
            if (!StringUtils.hasText(beanName)) {
                try {
                    if (containingBean != null) {
                        beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, this.readerContext.getRegistry(), true);
                    } else {
                        beanName = this.readerContext.generateBeanName(beanDefinition);
                        String beanClassName = beanDefinition.getBeanClassName();
                        if (beanClassName != null && beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
                                !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                            aliases.add(beanClassName);
                        }
                    }
                } catch (Exception ex) {
                    error(ex.getMessage(), ele);
                    return null;
                }
            }

            String[] aliasesArray = StringUtils.toStringArray(aliases);
            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
        }
        return null;
    }

    public void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
        String foundName = null;

        if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
            foundName = beanName;
        }
        if (foundName == null) {
            foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
        }
        if (foundName != null) {
        }

        this.usedNames.add(beanName);
        this.usedNames.addAll(aliases);
    }

    public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName, BeanDefinition containingBean) {

        this.parseState.push(new BeanEntry(beanName));
        String className = null;
        if (ele.hasAttribute("class")) {
            className = ele.getAttribute("class").trim();
        }
        String parent = null;
        if (ele.hasAttribute("parent")) {
            parent = ele.getAttribute("parent");
        }

        try {
            AbstractBeanDefinition bd = createBeanDefinition(className, parent);

            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, "description"));

            parseMetaElements(ele, bd);
            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

            parseConstructorArgElements(ele, bd);
            parsePropertyElements(ele, bd);
            parseQualifierElements(ele, bd);

            bd.setResource(this.readerContext.getResource());
            bd.setSource(extractSource(ele));

            return bd;
        } catch (Exception ex) {

        } finally {
            this.parseState.pop();
        }

        return null;
    }

    protected AbstractBeanDefinition createBeanDefinition(String className, String parentName) throws ClassNotFoundException {
        return BeanDefinitionReaderUtils.createBeanDefinition(parentName, className, this.readerContext.getBeanClassLoader());
    }

    public void parseBeanDefinitionAttributes(Element ele, String beanName, BeanDefinition containingBean, AbstractBeanDefinition bd) {

        if (ele.hasAttribute("singleton")) {
            error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
        } else if (ele.hasAttribute("scope")) {
            bd.setScope(ele.getAttribute("scope"));
        } else if (containingBean != null) {
            bd.setScope(containingBean.getScope());
        }

        if (ele.hasAttribute("abstract")) {
            bd.setAbstract("true".equals(ele.getAttribute("abstract")));
        }

        String lazyInit = ele.getAttribute("lazy-init");
        if (isDefaultValue(lazyInit)) {
            lazyInit = this.defaults.getLazyInit();
        }
        bd.setLazyInit("true".equals(lazyInit));
    }

    public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
        NodeList nl = ele.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, "meta")) {
                Element metaElement = (Element) node;
                String key = metaElement.getAttribute("key");
                String value = metaElement.getAttribute("value");
                BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
                attribute.setSource(extractSource(metaElement));
                attributeAccessor.addMetadataAttribute(attribute);
            }
        }
    }

    public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, "lookup-method")) {
                Element ele = (Element) node;
                String methodName = ele.getAttribute("name");
                String beanRef = ele.getAttribute("bean");
                LookupOverride override = new LookupOverride(methodName, beanRef);
                override.setSource(extractSource(ele));
                overrides.addOverride(override);
            }
        }
    }

    public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, "replaced-method")) {
                Element replacedMethodEle = (Element) node;
                String name = replacedMethodEle.getAttribute("name");
                String callback = replacedMethodEle.getAttribute("replacer");
                ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
                // Look for arg-type match elements.
                List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, "arg-type");
                for (Element argTypeEle : argTypeEles) {
                    String match = argTypeEle.getAttribute("match");
                    match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
                    if (StringUtils.hasText(match)) {
                        replaceOverride.addTypeIdentifier(match);
                    }
                }
                replaceOverride.setSource(extractSource(replacedMethodEle));
                overrides.addOverride(replaceOverride);
            }
        }
    }

    public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, "constructor-arg")) {
                parseConstructorArgElement((Element) node, bd);
            }
        }
    }


    public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
        String indexAttr = ele.getAttribute("index");
        String typeAttr = ele.getAttribute("type");
        String nameAttr = ele.getAttribute("name");
        if (StringUtils.hasLength(indexAttr)) {
            try {
                int index = Integer.parseInt(indexAttr);
                if (index < 0) {
                    error("'index' cannot be lower than 0", ele);
                } else {
                    try {
                        this.parseState.push(new ConstructorArgumentEntry(index));
                        Object value = parsePropertyValue(ele, bd, null);
                        ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                        if (StringUtils.hasLength(typeAttr)) {
                            valueHolder.setType(typeAttr);
                        }
                        if (StringUtils.hasLength(nameAttr)) {
                            valueHolder.setName(nameAttr);
                        }
                        valueHolder.setSource(extractSource(ele));
                        if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
                            error("Ambiguous constructor-arg entries for index " + index, ele);
                        } else {
                            bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
                        }
                    } finally {
                        this.parseState.pop();
                    }
                }
            } catch (NumberFormatException ex) {
                error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
            }
        } else {
            try {
                this.parseState.push(new ConstructorArgumentEntry());
                Object value = parsePropertyValue(ele, bd, null);
                ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                if (StringUtils.hasLength(typeAttr)) {
                    valueHolder.setType(typeAttr);
                }
                if (StringUtils.hasLength(nameAttr)) {
                    valueHolder.setName(nameAttr);
                }
                valueHolder.setSource(extractSource(ele));
                bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
            } finally {
                this.parseState.pop();
            }
        }
    }

    public Object parsePropertyValue(Element ele, BeanDefinition bd, String propertyName) {
        String elementName = (propertyName != null ?
                "<property> element for property '" + propertyName + "'" :
                "<constructor-arg> element");

        // Should only have one child element: ref, value, list, etc.
        NodeList nl = ele.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element && !nodeNameEquals(node, "description") &&
                    !nodeNameEquals(node, "meta")) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error(elementName + " must not contain more than one sub-element", ele);
                } else {
                    subElement = (Element) node;
                }
            }
        }

        boolean hasRefAttribute = ele.hasAttribute("ref");
        boolean hasValueAttribute = ele.hasAttribute("value");
        if ((hasRefAttribute && hasValueAttribute) ||
                ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
            error(elementName +
                    " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
        }

        if (hasRefAttribute) {
            String refName = ele.getAttribute("ref");
            if (!StringUtils.hasText(refName)) {
                error(elementName + " contains empty 'ref' attribute", ele);
            }
            RuntimeBeanReference ref = new RuntimeBeanReference(refName);
            ref.setSource(extractSource(ele));
            return ref;
        } else if (hasValueAttribute) {
            TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute("value"));
            valueHolder.setSource(extractSource(ele));
            return valueHolder;
        } else if (subElement != null) {
            return parsePropertySubElement(subElement, bd);
        } else {
            // Neither child element nor "ref" or "value" attribute found.
            error(elementName + " must specify a ref or value", ele);
            return null;
        }
    }

    public Object parsePropertySubElement(Element ele, BeanDefinition bd) {
        return parsePropertySubElement(ele, bd, null);
    }

    public Object parsePropertySubElement(Element ele, BeanDefinition bd, String defaultValueType) {
        if (!isDefaultNamespace(ele)) {

            // TODO
            return parseNestedCustomElement(ele, bd);
        } else if (nodeNameEquals(ele, "bean")) {
            BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
            if (nestedBd != null) {
                nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
            }
            return nestedBd;
        } else if (nodeNameEquals(ele, "ref")) {
            String refName = ele.getAttribute("bean");
            boolean toParent = false;
            if (!StringUtils.hasLength(refName)) {
                // A reference to the id of another bean in a parent context.
                refName = ele.getAttribute("parent");
                toParent = true;
                if (!StringUtils.hasLength(refName)) {
                    error("'bean' or 'parent' is required for <ref> element", ele);
                    return null;
                }
            }
            if (!StringUtils.hasText(refName)) {
                error("<ref> element contains empty target attribute", ele);
                return null;
            }
            RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
            ref.setSource(extractSource(ele));
            return ref;
        } else if (nodeNameEquals(ele, "idref")) {
            return parseIdRefElement(ele);
        } else if (nodeNameEquals(ele, "value")) {
            return parseValueElement(ele, defaultValueType);
        } else if (nodeNameEquals(ele, "null")) {
            TypedStringValue nullHolder = new TypedStringValue(null);
            nullHolder.setSource(extractSource(ele));
            return nullHolder;
        } else if (nodeNameEquals(ele, "array")) {
            return parseArrayElement(ele, bd);
        } else if (nodeNameEquals(ele, "list")) {
            return parseListElement(ele, bd);
        } else if (nodeNameEquals(ele, "set")) {
            return parseSetElement(ele, bd);
        } else if (nodeNameEquals(ele, "map")) {
            return parseMapElement(ele, bd);
        } else if (nodeNameEquals(ele, "props")) {
            return parsePropsElement(ele);
        } else {
            error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
            return null;
        }
    }

    private BeanDefinitionHolder parseNestedCustomElement(Element ele, BeanDefinition containingBd) {
        BeanDefinition innerDefinition = parseCustomElement(ele, containingBd);
        if (innerDefinition == null) {
            error("Incorrect usage of element '" + ele.getNodeName() + "' in a nested manner. " +
                    "This tag cannot be used nested inside <property>.", ele);
            return null;
        }
        String id = ele.getNodeName() + "#" + ObjectUtils.getIdentityHexString(innerDefinition);
        return new BeanDefinitionHolder(innerDefinition, id);
    }

    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
        String namespaceUri = getNamespaceURI(ele);
        if (namespaceUri == null) {
            return null;
        }
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
            return null;
        }
        return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
    }

    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef) {
        return decorateBeanDefinitionIfRequired(ele, originalDef, null);
    }

    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef, BeanDefinition containingBd) {

        BeanDefinitionHolder finalDefinition = originalDef;

        // Decorate based on custom attributes first.
        NamedNodeMap attributes = ele.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
        }

        // Decorate based on custom nested elements.
        NodeList children = ele.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
            }
        }
        return finalDefinition;
    }

    public BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef, BeanDefinition containingBd) {

        // TODO
        String namespaceUri = getNamespaceURI(node);
        if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
            NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
            if (handler != null) {
                BeanDefinitionHolder decorated =
                        handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
                if (decorated != null) {
                    return decorated;
                }
            } else if (namespaceUri.startsWith("http://www.springframework.org/")) {
                error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", null);
            } else {
            }
        }
        return originalDef;
    }

    public Object parseIdRefElement(Element ele) {
        // A generic reference to any name of any bean.
        String refName = ele.getAttribute("bean");
        if (!StringUtils.hasLength(refName)) {
            error("'bean' is required for <idref> element", ele);
            return null;
        }
        if (!StringUtils.hasText(refName)) {
            error("<idref> element contains empty target attribute", ele);
            return null;
        }
        RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
        ref.setSource(extractSource(ele));
        return ref;
    }

    public Object parseValueElement(Element ele, String defaultTypeName) {
        // It's a literal value.
        String value = DomUtils.getTextValue(ele);
        String specifiedTypeName = ele.getAttribute("type");
        String typeName = specifiedTypeName;
        if (!StringUtils.hasText(typeName)) {
            typeName = defaultTypeName;
        }
        try {
            TypedStringValue typedValue = buildTypedStringValue(value, typeName);
            typedValue.setSource(extractSource(ele));
            typedValue.setSpecifiedTypeName(specifiedTypeName);
            return typedValue;
        } catch (ClassNotFoundException ex) {
            error("Type class [" + typeName + "] not found for <value> element", ele);
            return value;
        }
    }

    public Object parseArrayElement(Element arrayEle, BeanDefinition bd) {
        String elementType = arrayEle.getAttribute("value-type");
        NodeList nl = arrayEle.getChildNodes();
        ManagedArray target = new ManagedArray(elementType, nl.getLength());
        target.setSource(extractSource(arrayEle));
        target.setElementTypeName(elementType);
        target.setMergeEnabled(parseMergeAttribute(arrayEle));
        parseCollectionElements(nl, target, bd, elementType);
        return target;
    }

    public boolean parseMergeAttribute(Element collectionElement) {
        String value = collectionElement.getAttribute("merge");
        if (isDefaultValue(value)) {
            value = this.defaults.getMerge();
        }
        return "true".equals(value);
    }

    public List<Object> parseListElement(Element collectionEle, BeanDefinition bd) {
        String defaultElementType = collectionEle.getAttribute("value-type");
        NodeList nl = collectionEle.getChildNodes();
        ManagedList<Object> target = new ManagedList<>(nl.getLength());
        target.setSource(extractSource(collectionEle));
        target.setElementTypeName(defaultElementType);
        target.setMergeEnabled(parseMergeAttribute(collectionEle));
        parseCollectionElements(nl, target, bd, defaultElementType);
        return target;
    }

    public Set<Object> parseSetElement(Element collectionEle, BeanDefinition bd) {
        String defaultElementType = collectionEle.getAttribute("value-type");
        NodeList nl = collectionEle.getChildNodes();
        ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
        target.setSource(extractSource(collectionEle));
        target.setElementTypeName(defaultElementType);
        target.setMergeEnabled(parseMergeAttribute(collectionEle));
        parseCollectionElements(nl, target, bd, defaultElementType);
        return target;
    }

    public Map<Object, Object> parseMapElement(Element mapEle, BeanDefinition bd) {
        String defaultKeyType = mapEle.getAttribute("key-type");
        String defaultValueType = mapEle.getAttribute("value-type");

        List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, "entry");
        ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
        map.setSource(extractSource(mapEle));
        map.setKeyTypeName(defaultKeyType);
        map.setValueTypeName(defaultValueType);
        map.setMergeEnabled(parseMergeAttribute(mapEle));

        for (Element entryEle : entryEles) {
            // Should only have one value child element: ref, value, list, etc.
            // Optionally, there might be a key child element.
            NodeList entrySubNodes = entryEle.getChildNodes();
            Element keyEle = null;
            Element valueEle = null;
            for (int j = 0; j < entrySubNodes.getLength(); j++) {
                Node node = entrySubNodes.item(j);
                if (node instanceof Element) {
                    Element candidateEle = (Element) node;
                    if (nodeNameEquals(candidateEle, "key")) {
                        if (keyEle != null) {
                            error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
                        } else {
                            keyEle = candidateEle;
                        }
                    } else {
                        // Child element is what we're looking for.
                        if (nodeNameEquals(candidateEle, "description")) {
                            // the element is a <description> -> ignore it
                        } else if (valueEle != null) {
                            error("<entry> element must not contain more than one value sub-element", entryEle);
                        } else {
                            valueEle = candidateEle;
                        }
                    }
                }
            }

            // Extract key from attribute or sub-element.
            Object key = null;
            boolean hasKeyAttribute = entryEle.hasAttribute("key");
            boolean hasKeyRefAttribute = entryEle.hasAttribute("key-ref");
            if ((hasKeyAttribute && hasKeyRefAttribute) ||
                    (hasKeyAttribute || hasKeyRefAttribute) && keyEle != null) {
                error("<entry> element is only allowed to contain either " +
                        "a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
            }
            if (hasKeyAttribute) {
                key = buildTypedStringValueForMap(entryEle.getAttribute("key"), defaultKeyType, entryEle);
            } else if (hasKeyRefAttribute) {
                String refName = entryEle.getAttribute("key-ref");
                if (!StringUtils.hasText(refName)) {
                    error("<entry> element contains empty 'key-ref' attribute", entryEle);
                }
                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
                ref.setSource(extractSource(entryEle));
                key = ref;
            } else if (keyEle != null) {
                key = parseKeyElement(keyEle, bd, defaultKeyType);
            } else {
                error("<entry> element must specify a key", entryEle);
            }

            // Extract value from attribute or sub-element.
            Object value = null;
            boolean hasValueAttribute = entryEle.hasAttribute("value");
            boolean hasValueRefAttribute = entryEle.hasAttribute("value-ref");
            boolean hasValueTypeAttribute = entryEle.hasAttribute("value-type");
            if ((hasValueAttribute && hasValueRefAttribute) ||
                    (hasValueAttribute || hasValueRefAttribute) && valueEle != null) {
                error("<entry> element is only allowed to contain either " +
                        "'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
            }
            if ((hasValueTypeAttribute && hasValueRefAttribute) ||
                    (hasValueTypeAttribute && !hasValueAttribute) ||
                    (hasValueTypeAttribute && valueEle != null)) {
                error("<entry> element is only allowed to contain a 'value-type' " +
                        "attribute when it has a 'value' attribute", entryEle);
            }
            if (hasValueAttribute) {
                String valueType = entryEle.getAttribute("value-type");
                if (!StringUtils.hasText(valueType)) {
                    valueType = defaultValueType;
                }
                value = buildTypedStringValueForMap(entryEle.getAttribute("value-type"), valueType, entryEle);
            } else if (hasValueRefAttribute) {
                String refName = entryEle.getAttribute("value-ref");
                if (!StringUtils.hasText(refName)) {
                    error("<entry> element contains empty 'value-ref' attribute", entryEle);
                }
                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
                ref.setSource(extractSource(entryEle));
                value = ref;
            } else if (valueEle != null) {
                value = parsePropertySubElement(valueEle, bd, defaultValueType);
            } else {
                error("<entry> element must specify a value", entryEle);
            }

            // Add final key and value to the Map.
            map.put(key, value);
        }

        return map;
    }

    public Properties parsePropsElement(Element propsEle) {
        ManagedProperties props = new ManagedProperties();
        props.setSource(extractSource(propsEle));
        props.setMergeEnabled(parseMergeAttribute(propsEle));

        List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, "prop");
        for (Element propEle : propEles) {
            String key = propEle.getAttribute("key");
            // Trim the text value to avoid unwanted whitespace
            // caused by typical XML formatting.
            String value = DomUtils.getTextValue(propEle).trim();
            TypedStringValue keyHolder = new TypedStringValue(key);
            keyHolder.setSource(extractSource(propEle));
            TypedStringValue valueHolder = new TypedStringValue(value);
            valueHolder.setSource(extractSource(propEle));
            props.put(keyHolder, valueHolder);
        }

        return props;
    }

    public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, "property")) {
                parsePropertyElement((Element) node, bd);
            }
        }
    }

    public void parsePropertyElement(Element ele, BeanDefinition bd) {
        String propertyName = ele.getAttribute("name");
        if (!StringUtils.hasLength(propertyName)) {
            error("Tag 'property' must have a 'name' attribute", ele);
            return;
        }
        this.parseState.push(new PropertyEntry(propertyName));
        try {
            if (bd.getPropertyValues().contains(propertyName)) {
                error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
                return;
            }
            Object val = parsePropertyValue(ele, bd, propertyName);
            PropertyValue pv = new PropertyValue(propertyName, val);
            parseMetaElements(ele, pv);
            pv.setSource(extractSource(ele));
            bd.getPropertyValues().addPropertyValue(pv);
        } finally {
            this.parseState.pop();
        }
    }

    public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, "qualifier")) {
                parseQualifierElement((Element) node, bd);
            }
        }
    }

    public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
        String typeName = ele.getAttribute("type");
        if (!StringUtils.hasLength(typeName)) {
            error("Tag 'qualifier' must have a 'type' attribute", ele);
            return;
        }
        this.parseState.push(new QualifierEntry(typeName));
        try {
            AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
            qualifier.setSource(extractSource(ele));
            String value = ele.getAttribute("value");
            if (StringUtils.hasLength(value)) {
                qualifier.setAttribute("value", value);
            }
            NodeList nl = ele.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (isCandidateElement(node) && nodeNameEquals(node, "attribute")) {
                    Element attributeEle = (Element) node;
                    String attributeName = attributeEle.getAttribute("key");
                    String attributeValue = attributeEle.getAttribute("value");
                    if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
                        BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
                        attribute.setSource(extractSource(attributeEle));
                        qualifier.addMetadataAttribute(attribute);
                    } else {
                        error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
                        return;
                    }
                }
            }
            bd.addQualifier(qualifier);
        } finally {
            this.parseState.pop();
        }
    }

    protected Object extractSource(Element ele) {

        // TODO
        //return this.readerContext.extractSource(ele);
        return null;
    }

    protected TypedStringValue buildTypedStringValue(String value, String targetTypeName) throws ClassNotFoundException {

        ClassLoader classLoader = this.readerContext.getBeanClassLoader();
        TypedStringValue typedValue;
        if (!StringUtils.hasText(targetTypeName)) {
            typedValue = new TypedStringValue(value);
        } else if (classLoader != null) {
            Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
            typedValue = new TypedStringValue(value, targetType);
        } else {
            typedValue = new TypedStringValue(value, targetTypeName);
        }
        return typedValue;
    }


    // private

    private boolean isCandidateElement(Node node) {
        return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
    }

    protected void parseCollectionElements(NodeList elementNodes, Collection<Object> target, BeanDefinition bd, String defaultElementType) {

        for (int i = 0; i < elementNodes.getLength(); i++) {
            Node node = elementNodes.item(i);
            if (node instanceof Element && !nodeNameEquals(node, "description")) {
                target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
            }
        }
    }

    protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
        try {
            TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
            typedValue.setSource(extractSource(entryEle));
            return typedValue;
        } catch (ClassNotFoundException ex) {
            error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle);
            return value;
        }
    }

    protected Object parseKeyElement(Element keyEle, BeanDefinition bd, String defaultKeyTypeName) {
        NodeList nl = keyEle.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error("<key> element must not contain more than one value sub-element", keyEle);
                } else {
                    subElement = (Element) node;
                }
            }
        }
        if (subElement == null) {
            return null;
        }
        return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
    }

    private void error(String msg, Element source) {
        System.out.println(msg);
    }
}
