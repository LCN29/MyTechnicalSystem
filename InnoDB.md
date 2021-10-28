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





## Insert Buffer 

插入缓冲，也称之为 Insert Buffer。经常会理解插入缓冲时缓冲池的一个部分，这样的理解是片面的，Insert Buffer 的信息一部分在内存中，另外一部分像数据页一样，存在于物理页中。

主键是行唯一的标识符，在应用程序中行记录的插入顺序是按照主键递增的顺序进行插入的。因此，插入聚集索引一般是顺序的，不需要磁盘的随机读取。

id 列是自增长的，这意味着当执行插入操作时，id 列会自动增长，页中的行记录按 id 执行顺序存放。一般情况下，不需要随机读取另一页执行记录的存放。因此，在这样的情况下，插入操作一般很快就能完成。

但是每张表上不可能只有一个聚集索引，在更多的情况下，一张表上有多个非聚集的辅助索引(二级索引)。   
大部分情况下, 一张表中存在一个以上的非聚集的并且不是唯一的索引, 在进行插入操作时，数据页的存放还是按主键 id 的执行顺序存放，但是对于非聚集索引，叶子节点的插入不再是顺序的了。  
这时就需要离散地访问非聚集索引页，插入性能在这里变低了, 因为 B+ 树的特性决定了非聚集索引插入的离散性。

InnoDB 存储引擎开创性地设计了插入缓冲, 对于非聚集索引的插入或更新操作，不是每一次直接插入索引页中，而是先判断插入的非聚集索引页是否在缓冲池中。
    如果在，则直接插入, 
    如果不在，则先放入一个插入缓冲区中, 然后再以一定的频率执行插入缓冲和非聚集索引页子节点的合并操作，这时通常能将多个插入合并到一个操作中（因为在一个索引页中），这就大大提高了对非聚集索引执行插入和修改操作的性能    


插入缓冲的使用需要满足以下两个条件：
> 1. 索引是辅助索引
> 2. 索引不是唯一的

当满足以上两个条件时，InnoDB存储引擎会使用插入缓冲，这样就能提高性能了。

有一种可能性:   
应用程序执行大量的插入和更新操作，这些操作都涉及了不唯一的非聚集索引，如果在这个过程中数据库发生了宕机，这时候会有大量的插入缓冲并没有合并到实际的非聚集索引中。  
如果是这样，恢复可能需要很长的时间，极端情况下甚至需要几个小时来执行合并恢复操作。

辅助索引不能是唯一的，因为在把它插入到插入缓冲时， 我们并不去查找索引页的情况。如果是唯一性的, 需要校验唯一性，就会出现离散读的情况，插入缓冲就失去了意义。

## Changer Buffer

最新的 MySQL5.7 已经支持 change buffer, 可以理解为 insert buffer 的升级, 也就是对常见的DML语言都可以进行缓冲，包含 insert delete 以及 update，对应的分别是 insert buffer，delete buffer 以及 purge buffer。

change buffer 的使用对象仍然是非唯一的辅助索引。


Update Demo

第一个部分是将记录的 delete_mask 标记为删除, delete buffer 对应的是update的第一个过程
第二个部分是真正的将记录删除, purge buffer对应的是第二个部分


insert buffer 的数据结构是一棵 B+ 树, 全局只有一棵 insert buffer B+ 树, 它负责对所有的表进行 insert buffer，而这棵 B+ 树放在共享表空间中。  
非叶子节点存放的是查询的search key值，它的构造如下：  space - mark - offst - metadata

这个结构一共占用 9 个字节，其中，space 表示待插入的记录所在的表的表空间 id，这个 id 是每个表都要有的唯一的 id, 其中 space 占用 4 个字节，marker 占用 1 个字节，  
用来兼容老版本的 insert buffer，offset 占用 4 个字节，表示页所在的偏移量。
metadata 占用 4 个字节，它用来排序每个记录进入 insert buffer 的顺序。

过程:  

当一个辅助索引要插入到数据页的时候，如果这个数据页不在缓冲池中，那么innodb会根据规则构造一个search key，接下来将这个记录插入到insert buffer的B+树里面去

为了保证每次merge insert buffer成功，需要设置一个特殊的数据页来标记每个辅助索引页的可用空间，这个数据页的类型为insert buffer bitmap，这个页可以追踪很多辅助索引页的可用空间


Merged Insert Buffer 的时机 ？

辅助索引页被读取到缓冲池的时候
insert buffer Bitmap 追踪到该辅助索引页已经没有足够的可用空间时，一般的阈值是辅助索引页空间的 1/32
master thread 每秒执行一次 merge insert buffer 的操作


## 两次写

插入缓冲带给 InnoDB 存储引擎的是性能，那么两次写带给 InnoDB 存储引擎的是数据的可靠性。

当数据库宕机时，可能发生数据库正在写一个页面，而这个页只写了一部分（比如16K的页，只写前4K的页）的情况，我们称之为部分写失效（partial page write）。


Redo log 中记录的是对页的物理操作, 物理页操作。  
如果这个页本身已经损坏，再对其进行重做是没有意义的。  

在应用（apply）Redo log 前，我们需要一个页的副本，当写入失效发生时，先通过页的副本来还原该页，再进行重做，这就是 doublewrite.

doublewrite 由两部分组成：
一部分是内存中的 doublewrite buffer，大小为 2MB
另一部分是物理磁盘上共享表空间中连续的 128 个页，即两个区（extent），大小同样为 2MB (页的副本) 

当缓冲池的脏页刷新时，并不直接写磁盘，而是会通过memcpy函数将脏页先拷贝到内存中的doublewrite buffer





https://www.cnblogs.com/wade-luffy/default.html?page=18
https://mp.weixin.qq.com/s/-puz311svMVbBAdRioPrnQ
https://www.cnblogs.com/bdsir/p/8745553.html
https://dev.mysql.com/doc/refman/8.0/en/innodb-in-memory-structures.html