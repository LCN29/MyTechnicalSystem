
Spring 中的类的大体情况

1. 接口 定义大体的行为, 提供一个抽象类, 对接口中一些可有共用的实现进行实现, 然后具体的实现类, 有时还会提供一个默认的实现 Default**
2. 通过组合模式, 对功能进行封装。 而不是通过继承

IOC 的过程

1. XML 文件路径  --> Resource --> BeanDefinition --> Bean


第一步
XML 文件路径 --> Resource 


Resource    --->     资源的封装

ResourceLoader  ---> 将 XML 文件加载为 Resource

BeanDefinitionReader ---> 执行者

PathMatcher  ---> 支持表达格式的资源解析


第二步 

Resource --->  BeanDefinition 


BeanDefinitionReader   --->  BeanDefinition 的读取  AbstractBeanDefinitionReader 抽象实现

BeanDefinitionDocumentReader

DefaultBeanDefinitionDocumentReader

XmlReaderContext

    XML 的读取

        将 Resource 组合为 EncodedResource, 带编码的 Resource, 后面的 Steam 用的 

        提取出里面的 InputSteam, 转为 InputSource  Spring 内部是使用 SAX 的方式进行 XML 文件的解析的, InputSource 是其输入源

            Sax 根据 InputSource 转为 Document, 解析



读取到的 BeanDefinition 在外面包一层为 BeanDefinitionHolder, 将 BenDefinition, beanName, 别名数组 组装成 BeanDefinitionHolder


获取 BeanDefinitionHolder 的 beanName 和 BeanDefinition 注册到 DefaultListableBeanFactory beanDefinitionMap

List<String> beanDefinitionNames 将注册的 beanName 存入这个 list

 










