# 


new ClassPathXmlApplicationContext 的过程

1. AbstractApplicationContext 的构造函数, 声明了资源解析器, 创建了 resourcePatternResolver = new PathMatchingResourcePatternResolver(this); 
2. 如果有父级的话，把父级的环境配置和当前的环境(当前没有的话，创建一个 StandardEnvironment 环境)配置合并
3. 将传入的配置路径，进行解析, 默认情况是调用到 AbstractEnvironment.resolveRequiredPlaceholders(String text) 进行路径解析

默认为 PropertySourcesPropertyResolver.resolveRequiredPlaceholders 流程  ---> 默认为 AbstractPropertyResolver的resolveRequiredPlaceholders
	1. 先创建一个 PropertyPlaceholderHelper
	2. 调用这个 PropertyPlaceholderHelper 进行文本解析
	3. 解析后，把路径放在 AbstractRefreshableConfigApplicationContext 的 configLocations 数组中

配置文件可以支持 ${} 的操作，会在项目启动时，进行替换

4. 进行 refresh() 操作
	
refresh 的流程

prepareRefresh  --> prepareBeanFactory -->  postProcessBeanFactory -->  invokeBeanFactoryPostProcessors -->  registerBeanPostProcessors -->
initMessageSource -->  onRefresh -->  registerListeners -->  finishBeanFactoryInitialization -->  finishRefresh


## 1. prepareRefresh 

>1. initPropertySources  protected 方法，子类进行重写
>2. 调用 当前的环境  StandardEnvironment 的 validateRequiredProperties 进行必要属性的校验
>3. 如果 earlyApplicationListeners 为空，创建 earlyApplicationListeners 然后放入 ApplicationListener 应用监听器
>4. 声明 earlyApplicationEvents 事件监听器列表

## 2. prepareBeanFactory

创建一个可以配置，可列举的 bean 工厂


1. refreshBeanFactory
	
	如果当前已经有 beanFactory 进行消毁，关闭



2. getBeanFactory
