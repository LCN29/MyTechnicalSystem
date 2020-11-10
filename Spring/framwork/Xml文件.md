
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


