# 内存结构

Buffer Pool
Change Buffer
Adaptive Hash Index
Log Buffer (主要是 Redo Log Buffer)


File Header (38 byte)
Page Header (56 byte)
Infimun Record + Suprenum Record (26 byte)
User Record (大小不定, 根据数据量伸缩)
Free Space (大小不定, 根据数据量伸缩)
Page Directory (大小不定, 根据数据量伸缩)
File Tailer (8 byte)


通过FileHeader中的上一下和下一页的数据，页与页之间可以形成双向链表

行与行之间则形成了单向链表。我们存入的行数据最终会到User Records中

https://home.cnblogs.com/u/wade-luffy/
https://mp.weixin.qq.com/s/-puz311svMVbBAdRioPrnQ
https://www.cnblogs.com/bdsir/p/8745553.html
https://dev.mysql.com/doc/refman/8.0/en/innodb-in-memory-structures.html