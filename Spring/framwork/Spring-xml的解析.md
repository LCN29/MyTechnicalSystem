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


	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {

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
							"Unable to validate using XSD: Your JAXP provider [" + factory +
							"] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
							"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}


	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
			@Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
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