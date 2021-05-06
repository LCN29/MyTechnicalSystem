# Redis

全称: Remote Dicrionary Service  远程字典服务

## 1. 修改参数
>1. 启动时携带参数
>2. 修改配置文件, 启动
>3. 通过命令动态修改 config set

## 2. 数据库的数量
默认为 16 个, 可以通过修改配置文件中的 **databases 数量** 就行修改。  
注: 这些数据库之间没有做到完全的隔离, 不像关系型数据库完全隔离 

## 3. 数据类型 
Binary-safe String
Hashes
Lists
Sets
Sorted sets

Bit arrays
HyperLogLogs
Steams 

## 4. String
存储的数据类型
int
float
string

## 数据结构

hashtable


源码内部实现
```C
typedef struct dictEntry {
    void *key;
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;
    struct dictEntry *next;
} dicEntry;
```

在 redis 中, key 值存储的结构为 SDS (Simple Dynamic String), 常用的 5 种数据类型的结果为 redisObject

```C
typedef struct redisObject {
    // 数据类型
    unsigned type:4;
    // 底层存储的数据结构编码
    unsigned encoding:4;
    unsigned lru:LRU_BITS;
    int refcount;
    void *ptr;
} robj;
``` 

可以通过 **type redisKey** 得到 redisKey 对应的 value 的数据类型
可以通过 **object encoding redisKey** 得到 redisKey 对应的 value 的在内存中适用的是什么样的编码(数据是如何组织存储在内存的, 而不是我们才是的 UTF 编码之类)

字符串的 encoding 有 int(8个字节的整形) embstr(字符串), raw(大于 44 个字节的字符串)

LRU_BITS: 内存回收策略
refcount  引用次数, 0 表示可以回收
prt  指向真正的数据 



redis 的 String 最大为 512 M, 当字符串长度小于 1M 时，扩容都是加倍现有的空间，如果超过 1M，扩容时一次只会多扩 1M 的空间。(字符串最大长度为 512M)

```C
static int checkStringLength(client *c, long long size) {
    // 超出了512M，就直接报错
    if (size > 512*1024*1024) {
        addReplyError(c,"string exceeds maximum allowed size (512MB)");
        return C_ERR;
    }
    return C_OK;
}
```








sds 在实际的存储中, 还要其他的类型

sdshdr5
sdshdr8
sdshdr16
sdshdr32
sdshdr64 

为什么?
C 语言中是没有 String 类型的, 只能通过 char[] 进行实现 (Java 的 String 内部也是通过一个 char[] 实现的)。

涉及到的问题
1. 涉及到了数组的话，就表示需要提前什么数组长度, 也就是分配内存空间。
2. 需要获取到长度, 就行进行遍历 O(n)
3. 字符串长度变化了，需要重新分配内存
4. C语言中通过 **\0** 代表字符串的结束, 存储其他的格式的内存，二进制表示的音频图片等,可能会出现问题，也就是 二进制不安全

解决上面的

```C
struct __attribute__ ((__packed__)) sdshdr8 {
     uint8_t len; // 使用到的长度
     uint8_t alloc; // 分配的长度
     unsigned char flags;
     char buf[];   // 数据的内容
};
```

字符串的长度可以通过 len 获取
allo, 分配的长度

flags: 字符串的类型
```C
#define SDS_TYPE_5  0
#define SDS_TYPE_8  1
#define SDS_TYPE_16 2
#define SDS_TYPE_32 3
#define SDS_TYPE_64 4
```

假设现在有一个字符串 sds* s, 通过 s[-1] 可以得到这个字符串的 flags, 可以推导出这个字符串的 len, alloc 等


字符串长度的获取 0(1), 有记录存储了
空间预分配, 惰性空间释放
字符串的结束通过判断，使用长度的数字进行判断， len


embstr 和 raw 的区别
embstr  分配内存是连续的
raw    不是连续的，需要分配 2 次

1. embstr 只读的, 如果对 embstr 的字符串进行改变, 会促使 embstr 立即变为 raw 
2. int 类型 超过了上限或者变为了不是数字时，会立即变为 raw
3. 编码之间的变化是不可逆的


string 的场景

1. 缓存
2. 分布式 Session
3. 分布式锁
4. incr 全局Id
5. incr 计数器, 阅读量等
6. incr 限流
7. setbit key 第几位 修改为: 1/0 位操作, 签到什么的

https://blog.csdn.net/qq193423571/article/details/81637075




