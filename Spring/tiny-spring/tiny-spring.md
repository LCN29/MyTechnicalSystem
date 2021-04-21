# tiny-spring

## step-01
将读取到的 Bean 定义 (Xml 文件, Java 代码) 转为 BeanDefinition。
根据 BeanDefinition 创建出对象, 并放入 BeanFactory, 内部为 key-value, key 为 bean name, value 为 bean 实例。

后面就能通过 bean name 从 BeanFactory 中获取需要的 bean。

## step-02

对 BeanFactory 进行抽象，抽象为一个接口，提供获取 Bean 的方法和注册 BeanDefinition 的方法

声明一个抽象类实现 BeanFactory, 实现 2 个方法, 但是让子类实现 BeanDefinition 到具体的 Bean 的过程

AutowireCapableBeanFactory 抽象类的实现之一

同时丰富了 BeanDefinition 的属性和方法

## step-03

给 Bean 注入属性, 每个 Bean 内部声明的每一个属性通过 PropertyValue 进行封装, 最后用一个 PropertyValues 将所有的 PropertyValue 包装起来, 不直接用一个 List, 因为通过一个包装类，内部可以提供一下方法操作, 比如重复的 PropertyValue 的
校验。这里的属性还不涉及到类, 而是一下 String, Integer 等能准确知道值的内容。

同时在 BeanDefinition 转为 Bean 后, 为 Bean 的属性赋值, 属性值同样来源于 BeanDefinition

