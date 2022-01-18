

```C
typedef struct client {

    // 递增的一个唯一 id, 通过全局变量 server.next_client_id 实现
    uint64_t id;  

    // 对应的 Socket 通道
    int fd;

    // 指向当前选中的数据库
    redisDb *db;

    // 客户端设置的名字, 可以通过命令 client setname 进行设置
    robj *name;  

    // 客户端输入缓冲区， recv 函数接收到客户端的数据会暂时缓存在这里
    sds querybuf; 

    // 指向客户端请求缓冲区已读的位置
    size_t qb_pos; 

    // If this client is flagged as master, 
    // this buffer represents the yet not applied portion of the replication stream that we are receiving from the master.
    // 如果此客户端被标记为主客户端，则此缓冲区表示我们从主服务器接收的复制流中尚未应用的部分。
    sds pending_querybuf; 

    // 最近 (100ms 或者更长) 查询到的输入缓冲区的峰值
    size_t querybuf_peak;

    // client 输入命令时，参数的数量
    int argc;

    // client 输入命令的参数列表
    robj **argv;

    // 客户端执行过的命令的历史记录
    struct redisCommand *cmd, *lastcmd;

    // 请求协议类型, 取值可以为 PROTO_REQ_INLINE 内联 或者 PROTO_REQ_MULTIBULK 多条命令
    int reqtype;

    // 参数列表中未读取命令参数的数量，读取一个，该值减 1
    int multibulklen;       

    // 命令内容的长度
    long bulklen;    

    // 回复缓存列表，用于发送大于固定回复缓冲区的回复       
    list *reply;

    // 回复缓存列表对象的总字节数
    unsigned long long reply_bytes;
    
    // 已发送的字节数或对象的字节数
    size_t sentlen; 

    // 客户端创建的时间
    time_t ctime;      

    // 最后一次和服务器交互的时间, 用来实现客户端 的超时处理。
    time_t lastinteraction; 

    // 服务器使用两种模式来限制客户端输出缓冲区的大小
    // 硬性限制：超过硬性限制，立即关闭客户端
    // 软性限制：超过软性限制，没有超过硬性限制
    // obuf_soft_limit_reached_time 属性记录客户端达到软性限制的起始时间
    // 之后服务器继续监视客户端，如果输出缓冲区的大小超出软性限制，并且持续时间超过服务器设定的时长，那么服务器将关闭客户端
    // 相反，在指定时间之内，不在超出软性限制，客户端就不会被关闭，并且 obuf_soft_limit_reached_time 属性的值也会被清0

    // 客户端的输出缓冲区超过软性限制的时间，记录输出缓冲区第一次到达软性限制的时间
    time_t obuf_soft_limit_reached_time;

    // client 状态的标志, 当前有 28 种, 具体可以查看 server.h 中 CLIENT_ 开始的常量定义
    int flags;              

    // 认证标志，0 表示未认证，1 表示已认证
    int authenticated; 

    // 从节点的复制状态
    int replstate;         

    // Install slave write handler on first ACK
    // 在 ack 上设置从节点的写处理器, 是否在 slave 向 master 发送 ack
    int repl_put_online_on_ack; 

    // 保存主服务器传来的 RDB 文件的文件描述符
    int repldbfd;           
    // 读取主服务器传来的 RDB 文件的偏移量
    off_t repldboff;       

    // 主服务器传来的 RDB 文件的大小
    off_t repldbsize;     

    // 主服务器传来的序言, 符合协议的字符串形式, 表示 RDB 文件大小的
    sds replpreamble;     

    // 如果当前的主服务器, 存储的是主从复制中的偏移量  
    long long read_reploff; 

    // 如果当前的主服务器, 存储的是复制的偏移量
    long long reploff;      

    // 如果是从节点, 存储的是通过 ack 命令接收到的偏移量
    long long repl_ack_off; 

    // 如果是从节点, 存储的是通过 ack 命令接收到的偏移量所用的时间
    long long repl_ack_time;

    // FULLRESYNC (全量赋值) 回复给从节点的offset
    long long psync_initial_offset; 

    // 如果是主服务器, 存储的是 run_id, 较新版本的 replication id 复制 ID
    char replid[CONFIG_RUN_ID_SIZE+1]; 

    // 从节点的端口号
    int slave_listening_port; 

    // 从节点 IP 地址
    char slave_ip[NET_IP_STR_LEN];

    // 从节点支持的复制的能力 psync 和 eof 2 种
    int slave_capa;         

    // 事物状态
    multiState mstate;     

    // 阻塞类型
    int btype;              

    // 阻塞的状态
    blockingState bpop;    

    // 最近一个写全局的复制偏移量
    long long woff;         

    // 事务中的监控列表
    list *watched_keys; 

    // 订阅频道
    dict *pubsub_channels;  

    // 订阅的模式
    list *pubsub_patterns;  

    // 被缓存的 ID
    sds peerid;          

    // 在当前客户端上的节点列表
    listNode *client_list_node; /* list node in client list */

    // 回复固定缓冲区的偏移量
    int bufpos;

    // 回复固定缓冲区
    // PROTO_REPLY_CHUNK_BYTES 默认等于 16*1024
    char buf[PROTO_REPLY_CHUNK_BYTES];


} client;
```

```C
// 事务状态
typedef struct multiState {
    
    // 事务命令队列数组
    multiCmd *commands;
    
    // 事务命令的总个数
    int count;        
    
    // 所有的命令的标识 (flags) 进行或操作得到的结果会保存在这里 cmd->flags | cmd2->flags
    // 这样可以很快的判断出保存的命令中是有某种类型的命令, 比如写命令, 读命令
    int cmd_flags;
                           
    // 同步复制的标识                      
    int minreplicas;        

    // 同步复制的超时时间
    time_t minreplicas_timeout;
} multiState;

// 事务命令
typedef struct multiCmd {
    
    // 命令的参数列表
    robj **argv;
    
    // 命令的参数个数
    int argc;

    // 命令执行的函数指针
    struct redisCommand *cmd;
} multiCmd;

```


```C
/**
 * 创建客户端
 * 
 * @param fd 连接对象的 Socket 通道的文件描述符
 */
client *createClient(int fd) {

    client *c = zmalloc(sizeof(client));

    initClientMultiState(c);
    return c;
}

/**
 *
 */
void initClientMultiState(client *c) {
    c->mstate.commands = NULL;
    c->mstate.count = 0;
    c->mstate.cmd_flags = 0;
}
```