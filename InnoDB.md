# 内存结构

Buffer Pool
Change Buffer
Adaptive Hash Index
Log Buffer (主要是 Redo Log Buffer)

## 页结构

File Header (38 byte)
Page Header (56 byte)
Infimun Record + Suprenum Record (26 byte)
User Records (大小不定, 根据数据量伸缩)
Free Space (大小不定, 根据数据量伸缩)
Page Directory (大小不定, 根据数据量伸缩)
File Tailer (8 byte)

默认情况下, 一页总共 16 k


通过 FileHeader 中的上一下和下一页的数据，页与页之间可以形成双向链表

行与行之间则形成了单向链表。我们存入的行数据最终会到 User Records 中

User Records 中的数据，是按照主键 id 来进行排序的，当我们按照主键来进行查找时，会沿着这个单向链表一直往后找



https://home.cnblogs.com/u/wade-luffy/
https://mp.weixin.qq.com/s/-puz311svMVbBAdRioPrnQ
https://www.cnblogs.com/bdsir/p/8745553.html
https://dev.mysql.com/doc/refman/8.0/en/innodb-in-memory-structures.html