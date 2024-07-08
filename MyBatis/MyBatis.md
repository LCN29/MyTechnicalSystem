## JDBC 例子

```java
    // JDBC driver name and database URL
    private final static String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private final static String DB_URL = "jdbc:mysql://localhost:3306/mybatis-study?characterEncoding=utf8";

    //  Database credentials
    private final static String USER = "root";
    private final static String PASS = "Li123456";

    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        try {

            Class.forName(JDBC_DRIVER);

            // 1. Connection
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            String sql = "select id, user_name, create_time from t_user where id=?";
            // 2. Statement
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, 1);

            // 3. ResultSet
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                long id = rs.getLong("id");
                String userName = rs.getString("user_name");
                Date createTime = rs.getDate("create_time");

                User user = new User();
                user.setUserName(userName);
                user.setId(id);
                user.setCreateTime(createTime);

                System.out.println(user);
            }

            rs.close();
            preparedStatement.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

```



配置文件例子:


Mybatis 本身的配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<!-- 配置文件的根元素 -->
<configuration>
    <!-- 属性: 定义配置外在化 -->
    <properties></properties>

    <!-- 设置: 定义mybatis的一些全局性设置 -->
    <settings>
       <!-- 具体的参数名和参数值 -->
       <setting name="" value=""/> 
    </settings>

    <!-- 类型名称: 为一些类定义别名 -->
    <typeAliases></typeAliases>

    <!-- 类型处理器: 定义Java类型与数据库中的数据类型之间的转换关系 -->
    <typeHandlers></typeHandlers>

    <!-- 对象工厂 -->
    <objectFactory type=""></objectFactory>

    <!-- 插件: mybatis 的插件,插件可以修改 mybatis 的内部运行规则 -->
    <plugins>
       <plugin interceptor=""></plugin>
    </plugins>

    <!-- 环境: 配置mybatis的环境 -->
    <environments default="">
       <!-- 环境变量: 可以配置多个环境变量，比如使用多数据源时，就需要配置多个环境变量 -->
       <environment id="">
          <!-- 事务管理器 -->
          <transactionManager type=""></transactionManager>
          <!-- 数据源 -->
          <dataSource type=""></dataSource>
       </environment> 
    </environments>

    <!-- 数据库厂商标识 -->
    <databaseIdProvider type=""></databaseIdProvider>

    <!-- 映射器: 指定映射文件或者映射类 -->
    <mappers></mappers>
    
</configuration>
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.test.mapper.UserMapper">

   <cache ></cache>

   <resultMap id="result" type="com.test.entity.User">
      <id column="id" jdbcType="BIGINT" property="id"/>
      <result column="user_name" jdbcType="VARCHAR" property="userName"/>
      <result column="create_time" jdbcType="DATE" property="createTime"/>
      <!--<collection property="" select=""-->
   </resultMap>

   <select id="selectById" resultMap="result"  >
        select id,user_name,create_time from t_user
        <where>
            <if test="id > 0"> and id=#{id} </if>
        </where>
    </select>
</mapper>
```






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

## 1 SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);

```java
public class Configuration {

    // properties 标签中的属性
    Properties variables;                                   // 解析后的属性列表

    // settings 标签 中的 logImpl 属性
    Class<? extends Log> logImpl;                           //  日志实现类
    // settings 标签 中的 logPrefix 属性
    Class<? extends VFS> vfsImpl;                           // 虚拟文件系统, 加载外部文件系统的配置, 基本很少用到
    // autoMappingBehavior 属性, 默认值 PARTIAL
    AutoMappingBehavior autoMappingBehavior;
    // autoMappingUnknownColumnBehavior 属性, 默认值 NONE
    AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior;
    // cacheEnabled 属性, 默认值 true
    boolean cacheEnabled;
    // proxyFactory 属性, 默认值 JavassistProxyFactory
    ProxyFactory proxyFactory;
    // lazyLoadingEnabled 属性, 默认值 false
    boolean lazyLoadingEnabled;
    // aggressiveLazyLoading 属性, 默认值 false
    boolean aggressiveLazyLoading;
    // multipleResultSetsEnabled 属性, 默认值 true
    boolean multipleResultSetsEnabled;
    // useColumnLabel 属性, 默认值 true
    boolean useColumnLabel;
    // useGeneratedKeys 属性, 默认值 false
    boolean useGeneratedKeys;
    // defaultExecutorType 属性, 默认值 SIMPLE
    ExecutorType defaultExecutorType;                       // 执行器类型, 默认为 Simple (reuse/batch)
    // defaultStatementTimeout 属性, 默认值 null
    Integer defaultStatementTimeout;
    // defaultFetchSize 属性, 默认值 null
    Integer defaultFetchSize;
    // defaultResultSetType 属性, 默认值 null
    ResultSetType defaultResultSetType;
    // mapUnderscoreToCamelCase 属性, 默认值 false
    boolean mapUnderscoreToCamelCase;
    // safeRowBoundsEnabled 属性, 默认值 false
    boolean safeRowBoundsEnabled;
    // localCacheScope 属性, 默认值 SESSION
    LocalCacheScope localCacheScope;
    // jdbcTypeForNull 属性, 默认值 OTHER
    JdbcType jdbcTypeForNull;
    // lazyLoadTriggerMethods 属性, 默认值 equals,clone,hashCode,toString
    Set<String> lazyLoadTriggerMethods;
    // safeResultHandlerEnabled 属性, 默认值 true
    boolean safeResultHandlerEnabled;
    // defaultScriptingLanguage 属性, 默认值 XMLLanguageDriver, 存放到了 LanguageDriverRegistry.LANGUAGE_DRIVER_MAP 和 defaultDriverClass 中
    LanguageDriverRegistry languageRegistry;                // 语言驱动注册器
    // defaultEnumTypeHandler 属性, 默认值 null, 存放在 TypeHandlerRegistry.defaultEnumTypeHandler
    TypeHandlerRegistry typeHandlerRegistry;                // 类型处理器注册器
    // callSettersOnNulls 属性, 默认值 false
    boolean callSettersOnNulls;
    // useActualParamName 属性, 默认值 false
    boolean useActualParamName;
    // returnInstanceForEmptyRow 属性, 默认值 false
    boolean returnInstanceForEmptyRow;
    // logPrefix 属性, 默认为 null
    String logPrefix;
    // configurationFactory 属性, 默认为 null
    Class<?> configurationFactory;


    // environments 标签
    // transactionManager 属性解析为对应的事务管理器, JdbcTransactionFactory 等, 存放在 Environment.transactionFactory
    // dataSource 属性解析为 PooledDataSourceFactory / UnpooledDataSourceFactory, 存放在 Environment.dataSource
    Environment environment;

    // databaseIdProvider 标签, 数据库厂商
    String databaseId;

    // typeAliases 标签,  解析到了 typeAliasRegistry.typeAliases 
    TypeAliasRegistry typeAliasRegistry;                    // 别名注册器

    // plugins 标签, 解析到了 interceptorChain.interceptors
    InterceptorChain interceptorChain;                      // 拦截器链

    // 下面 3 个工厂, 一般不会配置, 主要用于 反射实例化对象, 对象包装, 对象 get/set 方法的获取
    // objectFactory 标签
    ObjectFactory objectFactory;                            // 对象工厂
    // objectWrapperFactory 标签
    ObjectWrapperFactory objectWrapperFactory;              // 对象包装工厂
    // reflectorFactory 标签
    ReflectorFactory reflectorFactory;                      // 反射工厂

    // typeHandlers 标签, 存放到了 TypeHandlerRegistry.typeHandlerMap
    TypeHandlerRegistry typeHandlerRegistry;                // 类型处理器


    // mappers 标签, 存放到了 MapperRegistry.knownMappers
    MapperRegistry mapperRegistry;                          // Mapper 接口注册器
    // 已经解析完成的 Mapper 接口
    Set<String> loadedResources;
    // mapper 文件中的 resultMap 标签
    Map<String, ResultMap> resultMaps;

    // mapper 文件中的 select/update/delete/insert 标签解析成的对象
    Map<String, MappedStatement> mappedStatements;

}
```

## 2 SqlSession session = sqlSessionFactory.openSession();



```java
// configuration.environment.transactionFactory (默认: JdbcTransactionFactory, 兜底: ManagedTransactionFactory)
// level 事务隔离级别
Transaction tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
// 执行器, execType 默认为 SIMPLE (SimpleExecutor), BATCH (BatchExecutor), REUSE (ReuseExecutor)
// 如果开启了二级缓存, 原本的 executor 会被包装为 CacheExecutor 
Executor executor = configuration.newExecutor(tx, execType);
// 有配置拦截器, 进行代理
// 拦截器支持代理方法
// Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
// ParameterHandler (getParameterObject, setParameters)
// ResultSetHandler (handleResultSets, handleOutputParameters)
// StatementHandler (prepare, parameterize, batch, update, query)
executor = (Executor) interceptorChain.pluginAll(executor);

// 这个 对象就是调用方需要的 SqlSession
return new DefaultSqlSession(configuration, executor, autoCommit);   
```

## 3 BlogMapper mapper = session.getMapper(BlogMapper.class);

```java
   // configuration.mapperRegistry.knownMappers
   final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);

   // mapperProxyFactory.newInstance 方法内部逻辑
   final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
   return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
```


## 4  Blog blog2 = mapper.selectBlog(101);

```java

public class MapperProxy<T> implements InvocationHandler, Serializable {

   // Mapper 接口本身没有具体的实现, 都是 MyBatis 内部进行代理实现
   // 虽然看起来的调用了 BlogMapper 的接口, 但是实际当前的 mapper 的类型为 org.apache.ibatis.binding.MapperProxy
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
         // 判断方法是不是我们的 Object 类定义的方法，若是直接通过反射调用
         if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
         } else if (method.isDefault()) {   
            // 是否接口的默认方法
            // 是调用我们的接口中的默认方法
            return invokeDefaultMethod(proxy, method, args);
         }
      } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
      }

      // 将方法封装为 MapperMethod, 内部做了一个缓冲 methodCache, 存在直接返回, 否则创建
      // return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
      final MapperMethod mapperMethod = cachedMapperMethod(method);

      // 真正执行
      return mapperMethod.execute(sqlSession, args);
  }
}

// mapperMethod.execute(sqlSession, args); 省略其他非核心过程
public Object execute(SqlSession sqlSession, Object[] args) {

   switch (command.getType) {
      case insert/update/delete/flush:
         // 省略
         break;
      case select:
         if (method.returnsVoid() && method.hasResultHandler()) {
            // 返回值为空
         } else if () {
            // 返回值是一个 List/Map/Cursor
         } else {
            // 返回单个接口

            // 获取方法需要的参数, @Param 注解的干扰
            Object param = method.convertArgsToSqlCommandParam(args);
            result = sqlSession.selectOne(command.getName(), param);
         }
   }

   if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
}

// DefaultSqlSession.selectOne
public <T> T selectOne(String statement, Object parameter) {
   
   // 调用自身查询的 list
   // statment ==> Mapper 接口的全路径.方法名, 参数
   List<T> list = this.selectList(statement, parameter, RowBounds.DEFAULT);
   // 判断结果是否为 1 等逻辑
}

public <T> List<T> selectList() {

   // 获取配置的的 MappedStatement, 也就是 xml 中的方法
   MappedStatement ms = configuration.getMappedStatement(statement);
   // 调用执行器进行执行, 有拦截器先执行拦截器的逻辑, 然后执行 MappedStatement 的方法
   return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
}

public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
   
   // 获取需要执行的 SQL
   BoundSql boundSql = ms.getBoundSql(parameterObject);
   // 获取执行 SQL 的缓存 key, sql + 参数值 + 环境等拼接的一个 key
   CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
   // 判断是否开起来二级缓存, TransactionalCacheManager tcm; 获取, 没有进行执行

   // PerpetualCache localCache; 二级缓存
   Cache cache = ms.getCache();

   // 获得不到，则从数据库中查询
   list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);


   StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
   // 填充 jdbc 的 Connection, Statement 对象
   stmt = prepareStatement(handler, ms.getStatementLog());

   PreparedStatement ps = (PreparedStatement) statement;
   ps.execute();
   // 结果集转换
   return resultSetHandler.handleResultSets(ps);

}
```


## 解析

SQL 语句中存在 
${} -->   TextSqlNode
#{} -->  StaticTextSqlNode


insert/update/delete 解析 -->  org.apache.ibatis.scripting.xmltags.XMLLanguageDriver#createSqlSource(org.apache.ibatis.session.Configuration, org.apache.ibatis.parsing.XNode, java.lang.Class<?>)  

1. 解析为 DynamicSqlSource ( SQL 里面有 ${} ) /  RawSqlSource, 里面有个 SqlNode 属性, 具体类型为 MixedSqlNode 包含了整条 SQL 的所有内容
2. 和其他的参数统一封装为 MappedStatement 对象, 放到了 Configuration.mappedStatements 中 Map<String, MappedStatement> mappedStatements

## 执行

MappedStatement.getBoundSql(Object parameterObject)  -->  BoundSql 对象
> 1. 将参数和配置封装为一个 DynamicContext 对象
> 2. 调用 MappedStatement 里面的 SqlNode 对象 (Sql 标签的解析后的接口, 实际就是循环每个节点的 appLy 方法), 将 标签解析到 StringJoiner sqlBuilder 中, 这时参数还是 #{} 的形式
> 3. 通过 SqlSourceBuilder.parse 方法, 将 #{} 替换为 ?, 同时得到参数列表 List<ParameterMapping> parameterMappings, 包含了参数名, 参数类型 等信息, 最终得到一个 StaticSqlSource 对象
> 4. 通过 StaticSqlSource.getBoundSql 方法, 将配置, 最终的 SQL, 参数类型列表, 具体的参数, 封装为 BoundSql 对象


元素标签解析器
nodeHandlerMap.put("trim", new TrimHandler());
nodeHandlerMap.put("where", new WhereHandler());
nodeHandlerMap.put("set", new SetHandler());
nodeHandlerMap.put("foreach", new ForEachHandler());
nodeHandlerMap.put("if", new IfHandler());
nodeHandlerMap.put("choose", new ChooseHandler());
nodeHandlerMap.put("when", new IfHandler());
nodeHandlerMap.put("otherwise", new OtherwiseHandler());
nodeHandlerMap.put("bind", new BindHandler());


## 缓存

获取缓存 key  --> CacheKey, 格式: 偏移量 : 偏离量2: 接口全路径.方法名:?:sql语句:参数值:环境
二级缓存 存放地方 TransactionalCacheManager transactionalCaches
一级缓存 存放的地方 BaseExecutor 里面的 PerpetualCache localCache

## 数据库查询 BaseExecutor.queryFromDatabase
1. 从 MappedStatement 获取 Configuration, 通过 Configuration.newStatementHandler 获取 StatementHandler
2. 获取到 Connection 对象, 在通过 conn.prepareStatement(sql) 获取 PreparedStatement 对象, 实际 Connection/PreparedStatement 都是通过 MyBatis 将外面多加了一层代理后的对象 ConnectionLogger/PreparedStatementLogger, (主要是对日志进行了代理)
3. 将 PreparedStatement 放到 MappedStatement 中
4. 调用 MappedStatement 的 PreparedStatementLogger.execute 方法, 执行最终的 SQL 
5. ResultSetHandler.handleResultSets 对执行结果 ResultSet 进行转换 
