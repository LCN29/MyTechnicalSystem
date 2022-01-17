

```C
typedef struct client {

    // 递增的一个唯一 id
    uint64_t id;  

    // 对应的 Socket 通道
    int fd;

    // 指向当前选中的数据库
    redisDb *db;

    // 客户端设置的名字
    robj *name;  

    // 客户端请求缓冲区
    sds querybuf; 

    // 指向客户端请求缓冲区已读的位置
    size_t qb_pos; 

    


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