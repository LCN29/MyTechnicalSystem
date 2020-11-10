
# XML 文件

## 1. XML Demo
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xsi:schemaLocation="
	   http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.0.xsd">

	<!-- 声明一个 Java Bean	-->
	<bean id="personBean" class="com.can.spring.core.bean.Person">
		<property name="name" value="PersonBean"/>
		<property name="age" value="23"/>
	</bean>

</beans>
```

## 2. 一些单词的作用

1. version: 声明用的 XML 版本是 1.0
2. encoding: 声明用 XML 传输数据的时候的字符编码
3. xmlns: XML 命名空间(XML Namespace)

* 为什么需要 xmlns?  
假设加载了 2 个 XML 文件, 文件中里面同时有 <file> 这个标签, 这个标签可能带有不同内容和定义, 就会发生命名冲突, XML 解析器是无法确定如何处理这类冲突。为了解决上述问题，xmlns 就产生了。

* 如何使用 xmlns?
很简单 xmlns:namespace-prefix="namespaceURI", namespace-prefix 为自定义前缀，只要在这个 XML 文档中保证前缀不重复即可; namespaceURI 是这个前缀对应的 XML Namespace 的定义, 那么使用标签的时候，带上证前缀就行了。 例如上面的文件, 定义了 2 个 xmlns, 既 2 个命名空间, 第二个指定了前缀 `xsi`, 那么后续的使用到这个空间的标签, 都可以加上这个 `xsi`, 比如下面的 `xsi：schemaLocation` 就可以确定。备注如果指定命名空间时, 没有指定前缀的话，使用对应的标签，可以不使用前缀，如下面的 `bean`

* schemaLocation 标签的作用
定义了XML Namespace 和对应的 XSD (Xml Schema Definition) 文档的位置的关系, 格式为: `Namespace 对应的 Schema 文档的位置`, 可以有多对组合

## 3. XML 的校验

上面说到了 schemaLocation 的作用是绑定命名空间和他对应的 XSD 文档位置的关系。 那么什么是 XSD?

XSD 是 XML 文件常用的校验方式之一。 除了 XSD, 还有一张常用的校验方式 DTD。

* XML 文件 1
```xml
<?xml version="1.0" encoding="UTF-8"?>
<bean>
    <id>beanName</id>
    <class>com.can.Bean</class>
    <scope>singleton</scope>
</bean>
```

* XML 文件 2
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE bean PUBLIC "-//Spring//DTD BEAN 2.0//EN" "http://www.Springframework.org/dtd/Spring-beans-2.O.dtd">
<bean>
    <id>beanName</id>
    <class>com.can.Bean</class>
    <scope>singleton</scope>
</bean>
```

* XML 文件 3
```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:element name="bean">
    <xs:complexType>
        <xs:sequence>
            <xs:element name="id" type="xs:string" value="beanName"/>
            <xs:element name="class" type="xs:string" value="com.can.Bean"/>
            <xs:element name="scope" type="xs:string" value="singleton"/>
        </xs:sequence>
    </xs:complexType>
</xs:element>
```

>1. 第一个文件, 就是普通的一个 XML 文件。 而我们知道普通的 XML 的书写是很随意的, 元素名, 元素之间的嵌套都是没有约束的。

>2. 第二个文件, 则是一个加了 DTD(Document Type Definition) 校验的 XML 文件。  
DTD 是一种保证 XML 文档格式正确的有效验证机制, 可以通过比较 XML 文档和 DTD 文件来看文档是否符合规范, 元素和标签使用是否正确。  
一个 DTD 文档包含：元素的定义规则、元素间关系的定义规则、元素可使用的属性、可使用的实体或符号规则。它定义了 XML 文档相关的元素、  
属性、实体、排列方式、元素的内容类型以及元素的层次结构。它和普通的 XML 文件的最大的区别就是文件的开头有一个 **DOCTYPE** 的关键字。

虽然 DTD 可以校验 XML 文件的, 但是其本身有一定的缺陷
>>1. DTD 不遵守 XML 语法, 这导致解析策略（解析器, DOM、XPath等）难以重用
>>2. DTD 对元素类型支持有限, 不能自由扩充, 扩展性差
>>3. DTD 不支持命名空间
>>4. DTD 中所有元素、属性都是全局的, 无法声明仅与上下文位置相关的元素或属性

>3. 第三个文件, 则是一个遵循 XSD(XML Schemas Definition) 校验的 XML 文件。 XSD 是一种用来替代 DTD 的方案。相对于 DTD, XSD 具有如下优势

>>1. 基于 XML 没有专门的语法, 可以象其他 XML 文件一样解析和处理
>>2. 提供可扩充的数据模型
>>3. 支持综合命名空间, 支持属性组


## 4. 常用的 XML 解析方式

现在主流的解析 xml 的方式有 SAX, DOM, PULL

SAX(Simple API for XML) 使用流式处理的方式, 逐行读取文件内容, 然后基于回调的方式进行通知  

DOM(Document Object Model) 一次性全部将内容加载在内存中，生成一个树状结构,它没有涉及回调和复杂的状态管理

Pull 内置于 Android 系统中, 用于解析布局文件所使用的方式。 Pull 与 SAX 有点类似，都提供了类似的事件, Pull 解析器并没有强制要求提供触发的方法。 
因为他触发的事件不是一个方法，而是一个数字。 也就是在解析的过程中, 会将对应的事件转换为一个数字，每个数字对应的某个事件，比如最外面第一个节点, 节点的结束等。


在 Spring 使用的是 SAX 的方式进行文件的解析。

例子
```java
public static void saxReadXml(String xmlFilePath) throws Exception {

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // 不使用命名空间
    factory.setNamespaceAware(false);
    // 不启用校验
    factory.setValidating(false);
    MyHandler handler = new MyHandler();
    DocumentBuilder docBuilder = factory.newDocumentBuilder();
    // 提供一个如何寻找 DTD/XSD 校验文件的方式
    docBuilder.setEntityResolver(handler);
    // 解析异常时的处理方式
    docBuilder.setErrorHandler(handler);

	// 这一步就开始解析文件内容了
    Document doc = docBuilder.parse(new InputSource(xmlFilePath));
    
    // 获取根节点
    Element root = doc.getDocumentElement();
    // 读取跟节点下的所有节点列表
    NodeList nodeList = root.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (!(node instanceof Element)) continue;
        Element ele = (Element) node;
        if (!"bean".equals(ele.getNodeName())) continue;
        String id = ele.getAttribute("id");
        String clazz = ele.getAttribute("class");
        String scope = ele.getAttribute("scope");
        System.out.println("Result: beanName: " + id + ", beanClass: "+ clazz +", scope: " + scope);
    }
}


/**
 * 自定义事件实现类, DefaultHandler 是 SAX 提供的一个事件适配器, 可以继承这个, 实现自己关心的事件
 */
public static class MyHandler extends DefaultHandler {
    
    // 读取到 2 个标签的中间内容
    public void characters(char ch[], int start, int length) throws SAXException {
        String s = new String(ch, start, length);
        System.out.println(s);
    }

    // 开始读取到标签
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        System.out.println(localName + "///" + qName + "///" + uri + "////" + attrs.getValue("id"));
    }
}
``` 
以上就是 SAX 解析的方式


## 5. 参考
[XML 验证](https://www.runoob.com/xml/xml-dtd.html)
[Android之SAX、DOM和Pull解析XML](https://blog.csdn.net/qq_16628781/article/details/70147230)


