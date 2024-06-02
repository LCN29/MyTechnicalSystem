```java
public static void main(String[] args) throws IOException {

    String resource = "mybatis-config.xml";
    Reader reader = Resources.getResourceAsReader(resource);

    // 通过加载配置文件流构建一个 SqLSessionFactory 
    // 内部会对 xml 文件进行解析 (包括了数据库链接信息, 事务, 别名等配置)
    // XMLConfigBuilder  解析全局配置文件, 存放到一个 Configuration 
    // xml 文件中的标签列表 properties settings typeAliases plugins environment mappers
   
    // XMLMapperBuilder 解析 Mapper 文件
    // Mapper Class 的存放地 MapperRegistry
    // Mapper Class 的注解解析器 MapperAnnotationBuilder 
    // mapper 文件中的增删改查标签会构建为一个个 MappedStatement 对象 (MappedStatementBuilder 进行解析), 具体的 SQL 解析为 SqlSource (RawSqlSource, DynamicSqlSource($就为动态的), 判断会解析为 SqlNode),   在真正执行时变为 BoundSql 
    // mapper 文件中有 chace 标签, 使用二级缓存, Cache 对象 (实际是一系列的 Cahce 链, 委托下去给下一个 Cache)
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);

    // 获取一个 SqlSession
    SqlSession session = sqlSessionFactory.openSession();
    // 执行查询
    Blog blog = (Blog) session.selectOne("org.apache.ibatis.domain.blog.mappers.BlogMapper.selectBlog", 101);
    System.out.println(blog);

    BlogMapper mapper = session.getMapper(BlogMapper.class);
    Blog blog2 = mapper.selectBlog(101);
    System.out.println(blog2);

    session.close();
}
```