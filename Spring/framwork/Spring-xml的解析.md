# Spring xml 的解析


Spring 内部是使用 SAX(simple api for xml) 进行解析的。

相对于 DOM(一次性把xml文件加载到内存里，如果是大文件的话，很占内存，影响性能) 的解析方法, SAX 是事件驱动的流式解析方式，并不是把 xml 全部加载到内存，而是一边读取一边解析，不可暂停或者倒退，直到结束。

DOM解析器把XML文档转化为一个包含其内容的树，并可以对树进行遍历
DOM 在解析中可以对元素进行 crud, 而 SAX 不可以。


SAX 的简单使用
```java
/**
 * Defalulthandler 内部有好几个 public 但是无任何实现的方法，可以重写自身需要的，达到 xml 文件的解析
 */
public class MyHandler extends DefaultHandler {

	// 读取到 2 个节点之间的文本内容
	public void characters(char ch[], int start, int length) throws SAXException {
		String s = new String(ch, start, length);
		System.out.println(s);
	}

	// 读到节点
	public void startElement(String uri, String localName, String qName, Attributes attrs) {
		System.out.println(localName + "///" + qName + "///" + uri + "////" + attrs.getValue("id"));
	}

}


public void readXml() throws Exception {

	String xmlFilePath = "C:\\test.xml";
	SAXParserFactory sf = SAXParserFactory.newInstance();
	SAXParser sp = sf.newSAXParser();
	// sp 在在解析 xml 文件的时候，在对应的地方, 触发后面的 MyHandler 对应的事件
	sp.parse(new InputSource(xmlFilePath), new MyHandler());
}

```




```java

/** Document 用于读取 xml 的对象, 文档加载对象 */
public interface DocumentLoader {

	// namespaceAware: 是否启用 schema, 类似于一个限制，说明可以使用哪些属性，子元素之间的关系的 true:启用  默认为 false
	// validationMode: xml 文件校验模式, 1:不校验， 2: 自动检测验证模式  3: DTD 验证模式 4: XSD 验证模式 默认为 1
	Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception;
}


/**
 * Document 加载器
 */
public class DefaultDocumentLoader implements DocumentLoader {

	@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {

		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isTraceEnabled()) {
			logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		return builder.parse(inputSource);
	}


	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware) throws ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			factory.setValidating(true);
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				// Enforce namespace aware for XSD...
				factory.setNamespaceAware(true);
				try {
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				}
				catch (IllegalArgumentException ex) {
					ParserConfigurationException pcex = new ParserConfigurationException(
							"Unable to validate using XSD: Your JAXP provider [" + factory + "] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
							"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}


	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory, @Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
			throws ParserConfigurationException {

		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}

}
```

XmlBeanDefinitionReader  2 个作用，1个是 xml 的读取, 将 xml 中的 bean 转为 BeanDefinition


ResourceLoader  通过一个具体的位置加载为 Resource 
ResourcePatternResolver ResourceLoader 的扩展, 支持路径的模式匹配, 加载出 Resources[], 比如 Ant-style 风格


AbstractRefreshableApplicationContext 的 loadBeanDefinitions 方法中会调用 XmlBeanDefinitionReader
在调用的时候，放入 BeanDefinitionRegistry registy 
设置  Environment
设置  ResourceLoader 为 this（ClassPathXmlApplicationContext）
设置  EntityResolver 为 ResourceEntityResolver
设置  validationMode 为 自动检测, namespaceAware 命名空间检测为 false

然后通过  int loadBeanDefinitions(String... locations); 开始 xml 的解析, 加载 beanDefinitions

1. 先判断当前的 ResourceLoader 是否为 ResourcePatternResolver, 是的话，强转，解析为 Resource[] resources
2. 如果不是, 则直接调用 ResourceLoader 解析为 Resource resource
3. 统一调用自身的 loadBeanDefinitions(Resource... resource), 内部还是会调用的子类的 loadBeanDefinitions(Resource) 的实现类
也就是 XmlBeanDefinitionReader.loadBeanDefinitions(Resource);
4. 先将 Resource 转为 EncodedResource, 包装一层, 支持Resource 的解析时，指定编码和字符集
5. 从 EncodedResource 获取的输入流 InputStream, 在将 InputStream 封装为 InputSource, 这个就是 SAX 解析 xml 的数据源了
6. 在调用到自身的doLoadBeanDefinitions(InputSource inputSource, Resource resource) 
>>6.1 开始获取 Document
>>6.2 获取当前的验证模式, 如果一开始设置的不为VALIDATION_AUTO(1), 直接使用这个, 如果是自动判断, 最终从 VALIDATION_DTD(2) 和 VALIDATION_XSD(3) 从选择一个
>>6.3 检测的过程为, 逐行读取 xml 的每一行, 读到第一行为 `<真正的字符` 为止, 期间遇到了 `DOCTYPE` 关键字, 有的话为 DTD 验证，否则为 XSD 验证
>>6.4 调用 DefaultDocumentLoader 的 loadDocument(InputSource, EntityResolver, ErrorHandler, int validationMode, boolean namespaceAware); 获取到一个 Document
7. 拿到 Document, 其实到了这一步, xml 已经解析成功了
8. 通过 registerBeanDefinitions(Document, Resource), 开始将 Document 内的内容解析为 BeanDefinition
9. 

```java

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
}
```