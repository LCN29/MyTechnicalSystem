# MySQL

## explain 分析 SQL 的执行计划

关注的字段
> 1. id
> 2. type
> 3. possible_key 和 key
> 4. rows
> 5. filtered
> 6. Extra

id: 表示整个执行计划的执行顺序 id 值不同的时候, 先查询 id 值大的 (先大后小)
id 值相同时, 查询顺序是从上往下顺序执行

type: 对表访问方式, 表示 MySQL 在表中找到所需行的方式 常用的类型有: system > const > eq_ref > ref > range > index > all 性能从好到差

eq_ref 通常出现在多表的连接查询, 表示对于前表的每一个结果, 都只能匹配到后表的一行结果, 一般是唯一性索引的查询 
ref 查询用到了非唯一性索引, 或者关联操作只使用了索引的最左前缀 
range 使用索引进行范围查询 
index 索引全扫描 
all 全表扫描

一般保证查询至少达到 range 级别, 最好能达到 ref。const 和 eq_ref 可遇不可求

possible_key: 可能用到的索引, 如果是 NULL 就代表没有可以用的索引
key:  实际用到的索引, 如果是 NULL 就代表没有用到索引
rows: MySQL 认为扫描多少行才能返回请求的数据, 是一个预估值, 一般来说行数越少越好。
filtered: 存储引擎返回的数据在 server 层过滤后, 剩下多少满足查询的记录数量的比例, 它是一个百分比

Extra: 额外的信息说明, 个别情况说明

distinct: 一旦 MySQL 找到了与行相联合匹配的行, 就不再搜索了
index: 用到了覆盖索引, 不需要回表
Using where: 存储引擎返回的记录并不是所有的都满足查询条件, 需要 在 server 层进行再按照 where 后面的条件过滤 (跟是否使用索引没有关系)
Using temporary: MySQL 需要创建一张临时表来处理查询。出现这种情况一般要进行优化的, 首先是想到用索引来优化。
可能出现的情况
> 1. distinct 非索引列
> 2. group by 非索引列
> 3. 使用 join 的时候, group 任意列
Using filesort: 不能使用索引来排序, 用到了额外的排序 (跟磁盘或文件没有关系),  出现这种情况一般需要进行优化
可能出现的情况
> 1. 由于排序没有走索引、使用union、子查询连接查询、使用某些视图等原因
可以通过 order 索引解决


Using index condition: 索引条件下推

同一条 SQL 有多个索引使用时, 基于 cost 分析的

假设有两个索引 idx1(a, b, c), idx2(a, c)

select * from t where a = 1 and b in (1, 2) order by c  
--> 如果走 idx1，那么是 type 为 range
--> 如果走 idx2，那么 type 是 ref
当需要扫描的行数，使用 idx2 大约是 idx1 的 5 倍以上时，会用 idx1，否则会用 idx2

强制走某个索引 ---> from 表名 force index(索引名) where 条件

JOIN中的顺序选择
两层循环结构
驱动表 ---> 表中数据最小, 驱动表的字段它是可以直接排序的
非驱动表

表 1 STRAIGHT_JOIN 表2, 强制指定表2为驱动表

(left) join 多个表, 如果有不是直接 (left) join 主表的, 可以尝试将不直接 (left) join 主表的的优化为子查询, 前提是子表的字段没有作为查询条件 

```sql
SELECT  
    *  
FROM  
    rank_user AS rankUser  
LEFT JOIN rank_user_level AS userLevel ON rankUser.id = userLevel.user_id  
LEFT JOIN rank_product AS product ON userLevel.new_level = product.level_id  
LEFT JOIN rank_product_fee AS fee ON userLevel.fee_id = fee.fee_id  
LEFT JOIN rank_user_login_stat AS userLoginInfo ON rankUser.id = userLoginInfo.user_id  
ORDER BY  
     rankUser.create_time DESC  
LIMIT 10 OFFSET 0
```

```sql
SELECT  
            rankUser.id, rankUser.qq, rankUser.phone, rankUser.regip, rankUser.channel, rankUser.create_time, rankUser.qudao_key, rankUser.qq_openid, rankUser.wechat_openid,  
            userLevel.recommend_count,userLevel.end_time,userLevel.new_level,userLevel.`level`,userLevel.new_recommend_count,userLevel.`is_limited`,  
            (case when userLevel.new_level > 1 then 1 else 0 end) is_official_user,  
            (select product_name from rank_product where level_id = userLevel.new_level) product_name,  
            (select period from rank_product_fee where fee_id = userLevel.fee_id) period,  
            userLoginInfo.last_login, userLoginInfo.login_count, userLoginInfo.login_seconds  
        FROM rank_user AS rankUser  
        LEFT JOIN rank_user_level as userLevel on userLevel.user_id=rankUser.id  
        LEFT JOIN rank_user_login_stat as userLoginInfo ON rankUser.id = userLoginInfo.user_id  
ORDER BY  
    rankUser.create_time DESC  
LIMIT 10 OFFSET 0  
```

非直接关联转变成直接关联

```sql
SELECT  
    *  
FROM  
    rank_user AS rankUser  
LEFT JOIN (  
        select   
        l.*,p.product_name,f.period   
        from   
        rank_user_level l,rank_product p,rank_product_fee f  
        where   
        l.new_level = p.level_id   
        and l.fee_id = f.fee_id  
) AS userLevel ON rankUser.id = userLevel.user_id  
LEFT JOIN rank_user_login_stat AS userLoginInfo ON rankUser.id = userLoginInfo.user_id  
ORDER BY  
    rankUser.create_time DESC  
LIMIT 10 OFFSET 0  
```