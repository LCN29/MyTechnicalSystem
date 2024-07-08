

## 1 多数据源

### SpringJDBC 

1. 声明一个类实现 

### MyBatis

1. 声明 2 个配置类, 声明 2 个不同的数据源 + SqlSessionFactory, 同时通过 @MapperScan(basePackages = "com.tuling.datasource.dynamic.mybatis.mapper.r",
sqlSessionFactoryRef="rSqlSessionFactory")
2. 声明 2 个 Mapper 接口, 一个使用 @Mapper 注解, 一个使用 @MapperScan 注解

### dynamic-datasource-spring-boot-starter

借助第三方包


## 2 代码生成

<dependency>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
</dependency>

可以参考 mybatis-plus-generator 的实现

<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>3.5.7</version>
</dependency>

## 3 请求头透传
