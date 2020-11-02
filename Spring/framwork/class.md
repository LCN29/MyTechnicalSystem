

1. BeanDefinitionReader  存储 bean 的定义

BeanDefinitionReader
	- AbstractBeanDefinitionReader
		- XmlBeanDefinitionReader
		- PropertiesBeanDefinitionReader
		- GroovyBeanDefinitionReader



2. Environment 表示整个应用运行时的环境, 指应用环境的2个方面：profiles  和 properties

PropertyResolver
	- Environment
		- ConfigurableEnvironment
			- AbstractEnvironment
				- StandardEnvironment
					- StandardServletEnvironment
				- MockEnvironment					


