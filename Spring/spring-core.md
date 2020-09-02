# 


new ClassPathXmlApplicationContext 的过程

1. AbstractApplicationContext 的构造函数, 声明了资源解析器, 创建了 resourcePatternResolver = new PathMatchingResourcePatternResolver(this); 
2. 如果有父级的话，把父级的环境配置和当前的环境(当前没有的话，创建一个 StandardEnvironment 环境)配置合并
3. 将传入的配置路径，进行解析, 默认情况是调用到 AbstractEnvironment.resolveRequiredPlaceholders(String text) 进行路径解析

默认为 PropertySourcesPropertyResolver.resolveRequiredPlaceholders 流程  ---> 默认为 AbstractPropertyResolver的resolveRequiredPlaceholders
	1. 先创建一个 PropertyPlaceholderHelper
	2. 调用这个 PropertyPlaceholderHelper进行文本解析
	3. 解析后，把路径放在 AbstractRefreshableConfigApplicationContext 的 configLocations 数组中

配置文件可以支持 ${} 的操作，会在项目启动时，进行替换	

4. 进行 refresh() 操作


声明配置文件解析器，
解析为响应的路径，存到数组中
调用 refresh（）
