
从 xml 读取 bean 


```java
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
	// 将 Resource 包装为 EncodeResource, 这个本来是可以指定编码和字符集, 此处没指定，为 null
	return loadBeanDefinitions(new EncodedResource(resource));
}
```

```java
public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
	Assert.notNull(encodedResource, "EncodedResource must not be null");
	if (logger.isTraceEnabled()) {
		logger.trace("Loading XML bean definitions from " + encodedResource);
	}

	// 获取到当前线程内部维护的可以加载的资源
	Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();

	// 没有的话，初始一个默认的，并放到当前的 ThreadLocal
	if (currentResources == null) {
		currentResources = new HashSet<>(4);
		this.resourcesCurrentlyBeingLoaded.set(currentResources);
	}
	// 添加到集合中
	if (!currentResources.add(encodedResource)) {
		throw new BeanDefinitionStoreException("Detected cyclic loading of " + encodedResource + " - check your import definitions!");
	}

	try {
		// 默认为 BufferedInputStream
		InputStream inputStream = encodedResource.getResource().getInputStream();

		try {
			
			InputSource inputSource = new InputSource(inputStream);
			// 指定了编码
			if (encodedResource.getEncoding() != null) {
				inputSource.setEncoding(encodedResource.getEncoding());
			}
			return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
		} finally {
			inputStream.close();
		}

	} catch (IOException ex) {

		throw new BeanDefinitionStoreException("IOException parsing XML document from " + encodedResource.getResource(), ex);
	} finally {
		currentResources.remove(encodedResource);
		if (currentResources.isEmpty()) {
			this.resourcesCurrentlyBeingLoaded.remove();
		}	

	}

}
```

```java
protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource) throws BeanDefinitionStoreException {

	try {
		// 将 xml 解析为一个 Document 
		Document doc = doLoadDocument(inputSource, resource);
		int count = registerBeanDefinitions(doc, resource);
		return count;

	} catch (BeanDefinitionStoreException ex) {
		throw ex;
	} catch (SAXParseException ex) {
		throw new XmlBeanDefinitionStoreException(resource.getDescription(), "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
	} catch (SAXException ex) {
		throw new XmlBeanDefinitionStoreException(resource.getDescription(), "XML document from " + resource + " is invalid", ex);
	} catch (ParserConfigurationException ex) {
		throw new BeanDefinitionStoreException(resource.getDescription(), "Parser configuration exception parsing XML from " + resource, ex);
	} catch (IOException ex) {
		throw new BeanDefinitionStoreException(resource.getDescription(), "IOException parsing XML document from " + resource, ex);
	} catch (Throwable ex) {
		throw new BeanDefinitionStoreException(resource.getDescription(), "Unexpected exception parsing XML document from " + resource, ex);
	}

}
```

// 声明 XML Document 
```java

protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
	// xml 文件解析为一个Document文档
	return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler, getValidationModeForResource(resource), isNamespaceAware());
}
```

// 从 XML Document 中读取 Bean
```java
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {

	// 通过反射声明了 BeanDefinitionDocumentReader 对象
	BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
	// 获取当前解析成功的 Bean 个数
	int countBefore = getRegistry().getBeanDefinitionCount();
	// 创建 XmlReaderContext, 开始解析文本, 
	// 将 xml 中声明的 bean 变为 BeanDefinition, 存放到 DefaultListableBeanFactory 的 Map<String, BeanDefinition> beanDefinitionMap key 为 bean 的名字
	// 将所有的 bean name 放到 List<String> beanDefinitionNames, 同时在初时阶段就确保 beanName 唯一
	documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
	// 加载了多少个beanDefinition
	return getRegistry().getBeanDefinitionCount() - countBefore;
}
```



https://blog.csdn.net/qq_25179481/article/details/97976774

https://xie.infoq.cn/article/01bf9788500d5a98c11a5bf3b