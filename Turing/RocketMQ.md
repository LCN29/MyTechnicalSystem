
# 第一章

官网: https://rocketmq.apache.org/docs/

MQ 的三大作用:
异步, 削峰, 解耦

## Kafka 和 RocketMQ 比较

Kafka 有一个很大的弊端:
topic 太多, 消息吞吐性能。
一个 topic 下有多个 Partition, 文件按照 Partition 组织。
topic 增加 --> Partition 增加 --> 文件增加 --> 写文件性能下降, 写大量的索引。

适用场景: 日志场景下, topic 不需要很多。

RocketMQ   
适用场景 topic 很多, 业务场景多的情况。 topic --> 可以简单理解为业务场景多。

RabbitMQ 消息堆积影响性能

## Broker 
broker 常见问题
https://blog.csdn.net/hollis_chuang/article/details/127045946

namesever, broker(主从)
broker 将自己的信息注册到 namesever, Producer/Consumer 从 namesever 获取 Broker 信息

## MessageQueue
一个 Topic 多个 MessageQueue, 默认一队 Broker, 4 个队列。
MessageQueue 平均分配在不同的 Broker 集群中。

例子: 现在有 2 个 Broker 集群:

Broker-a-master  --> Broker-a-slave
Broker-b-master  --> Broker-b-slave

8 个队列的分配如下 (发送了 1000 条消息, 这些消息会平均到每个队列，每个队列 125 条): 

MessageQueue(brokerName=Broker-a, queueId=0)
MessageQueue(brokerName=Broker-b, queueId=0)
MessageQueue(brokerName=Broker-a, queueId=1)
MessageQueue(brokerName=Broker-b, queueId=1)
.....

## 管理后台里面 - Topic 菜单栏
状态:
offest(位点) 简单理解就是当前队列里面存储的消息量
min  最小消息， max  最大消息
路由: 
offest(位点) 简单理解就是消息量
min 已消费的最小消息，max 最大消息

消费者组消费的位点：已经记录在 Broker 中了， 下次启动就从这个位置开始消费
全新的消费者组, 可以从最新的消息开始消费

## 为什么不使用 Zookeeper 作为注册中心? 

namesever 之间无任何交互, 不存在脑裂,
cap cp, 数据一致, 没有保证 a, 可能存在一段时间的不可用
ap 在这里比较适合 nameServer, namesever 存放的只是各种 Broker, Topic 信息, 发送错误了, 做多一次兜底, 请求多一次 NameServer 即可

## Broker 高可用
Broker 主从，主挂了, 从无法升级为主，所以非高可用的

4.5.0 以后的真高可用方式 Dledger
接管 Broker 的 Commit Log 消息存储
从集群中选举 Master 节点
完成 master 节点往 slave 节点的消息同步

# 第二章

```java
public class MessageExt extends Message {

    private String brokerName;

    private int queueId;

    private int storeSize;

    private long queueOffset;

    ......
}
```
发送对象为 Message, 消费收到的对象为 MessageExt。


## 基本样例, 也就是基础功能

发送消息方式
同步发送 等待 MQ 响应 (Producer 内部会尝试进行重试) producer.send(msg), 阻塞
异步发送  发送消息到 MQ, 同时携带一个回调函数, 后续就不管了, MQ 接受到这个消息后, 回调这个函数 producer.send(msg, new SendCallback())
单向发送, 只推送消息到 MQ, 不需要确认消失是否到 MQ, producer.sendOneway(msg)

消费消息
主动拉: 
消费者主动到 Broker **拉消息** consumer.fetchSubscribeMessageQuery("Topic")
得到对应 tocpic 下的 MessageQueue, 死循环 MessageQueue, 获取里面的消息
备注: consumer.pullBlockIfNotFound(MessageQueue, subExpression, offset, maxNums) （DefaultLitePullConsumer 替代过期方法）

服务端主动推: 
Broker 主动**推消息**到消费者 (本质还是消费者拉)， consumer.registerMessageListener(new MessageListenerConcurrenly())

## 消息类型

顺序消息 (局部顺序，按照某个值, 发送到同一个队列, 同时需要消费端优先消费同一个队列的消息, 才能确定有序)
producer.send(msg, new MessageQueueSelector()) 生产端通过 消息选择器 指定消息推送到哪个 MessageQueue, 为顺序性提供条件
consumer.registerMessageListener(new MessageListenerQrderly()) 优先把同一个队列中的消息获取完

广播消息
上面的情况，一个消息只会由同一个消费组中的一个消费者消费
同一个消息，可以由所有的消费者进行消费 consumer.setMessageModel(broadcast)

延迟消息
msg.setDelayTimeLevel(3), 先发到系统内部自己维护的一个 schedule_topic_xxx 的队列, 定时任务, 移动到对应的正常的队列

批量消息
producer.send(List<Message>) 有消息大小限制, 建议 不超过 1M, 最大为 4M 
producer.send(ListSplitter<Message>) ListSplitter, 内部提供的类，会自定计算大小

过滤消息
tag 过滤 --> 完全匹配, 单维度
sql 过滤 --> 复杂过滤, 多维度
sql 的使用 --> **SQL92 语法使用**
consumer.subscribt("topic", MessageSelector.bySql("TAGS is not null and a is not null and a between 0 and 3")) 
指定消息的过滤条件: 消息的 tag 存在 同时消息里面有 a 这个属性, 值在 0 到 3 之间,
(生产者可以通过 msg.putUserProperty("a", "11") 进行设置) 

Broker 过滤 + Consumer 过滤, 共同配合完成过滤消息

事务消息 （事务消息只和生产者有关, 只保证了生产端的正常, 但是下游的消费失败等, 不受事务控制）
```java
// 事务发送者
TransactionMQProducer producer = new TransactionMQProducer("group");

// 设置回调监听器 内部 2 个方法
// executeLocalTransaction 告诉 Broker 是否执行事务， commit/rollback/unknown      
// checkLocalTransaction   检查本地事务是否执行成功      
producer.setTransactionListener(new TransactionListener());

// 发送消息
producer.sendMessageInTrasnaction(msg, null);
```

流程
1. 生产者发送消息，这个消息会变为 half 消息 (这个消息这时下游不可见, 放到了一个 RMQ_SYS_TRANS_HALF_TOPIC 的 Topic 中)
2. Broker 回复 half 消息,
3. 生产者执行自己的本地事务 (也就是自己的逻辑，比如入库什么的), 对应方法 executeLocalTransaction
4. 返回自己本地事务的处理状态(commit, rollback, unknown - 可能回查时，本地事务还为执行完，先返回 unknown)
   4.1 commit, 将消息发送给下游服务
   4.3 rollback, 丢弃消息
   4.4 unknown, 过一段时间再回查生产者的本地事务的状态 (checkLocalTransaction)
   4.5 生产者可以去检查自己本地事务的执行情况, 在向 Broker 返回一个 commit/rollback/unknown
   4.6 如果还是 unknown, 继续回查本地的事务, 最多尝试 15 次, 最终进行丢弃


demo
下单 -> 支付 -> 下游

下单前先发一个 mq, 下单(订单入库), 如果直接下单, 再发 MQ (可能存在 RocketMQ 挂了, 导致消息发送失败)
下完单后，进行支付, 支付成功(支付也进行了异步处理), 才推送下游, 这时可以先返回 unknown, 后面 Broker 进行回查，
再去查询支付状态, 确定 commit/rollback, 保证最终消失是否推送

简单粗暴，
同步发送 + 多次重试，也可以实现事务


## ACL Topic 权限配置

权限控制（ACL）主要为RocketMQ提供Topic资源级别的用户访问控制。用户在
使用RocketMQ权限控制时，可以在Client客户端通过 RPCHook注入AccessKey和
SecretKey签名；同时，将对应的权限控制属性（包括Topic访问权限、IP白名单和
AccessKey和SecretKey签名等）设置在$ROCKETMQ_HOME/conf/plain_acl.yml
的配置文件中。Broker端对AccessKey所拥有的权限进行校验，校验不过，抛出异常。

## RocketMQ 使用中常见的问题

### 哪些环节会有丢消息的可能
跨网络场景
> 1. 生产者 -> Broker  ==》 同步发送 + 多次重试(可以拿到响应结果) + 事务消息
> 2. Broker -> 消费者 ==》消费端 手动 ack (Broker 未收到 ack, 会重试 16 次)
> 3. Broker Master -> Broker Slave, 主到从消息同步方式
>> 3.1 同步双写 Master 每收到一条消息, 就同步一条到从节点 (主从同步完成了, 客户端才能收到响应结果)
>> 3.2 异步复制 Master 堆积一定量消息, 再同步给从节点

非跨网络场景
> 1. Broker -> 磁盘中间有系统级缓存, 真正同步到磁盘有时间差  ==> 同步刷盘
> 2. Broker 为了性能, 堆积一点消息后再刷入磁盘，异步刷盘     ==》 同步刷盘

也可以通过高版本的 Dledger 2阶段提交, 保存下来 uncommited, 返回客户端, 同步给其他节点，超过一半响应了 ack，变为 commited (基于 Raft 算法)

### 挤压问题解决
RocketMQ 抗挤压能力很强，一般情况不需要解决。

1 个 Queue 只会由一个消费者组中的 1 个 Consumer 进行消费 (Consumer 内部是多线程的), 不会出现 1 个 Queue 多个 Consumer 消费
所以，加消费者, 加到和 Queue 一样多

新增一个 Topic, 扩大队列, 启动一个消费者，只做消息搬运，将旧 Topic 的消息转发到新 Topic， 然后重复上面的步骤，增加消费者

# 第三章 

## 读队列 写队列
生产端消息会先发到写队列, 然后维护一份到读队列, 消费端从读队列获取消息, **读写分离思想**
写队列 --> 创建存储文件 
读队列 --> 维护 Consumer 的 offset
默认情况下 2 个是一一对应的，消息写入写队列的同时，会写入一份到读队列

写队列多
写队列没有读队列, 消费端无法获取到里面的消息, 消息丢失 - 消息存入了，但是读不出来

读队列多
读队列一定会分配 Consumer, 但是没有对应的写队列，也就是没有消息进入到这些读队列，造成 Consumer 空转, 极大的浪费性能

特殊情况：
队列缩容 (立即缩减读写队列, 被缩减的 MessageQueue 上没有被消费的消息，就会丢失)
先调整写队列数量, 读队列此时还有消息, 后面消费完了, 在进行缩读队列，达成平稳的过渡。

## 消息持久化

默认目录在 ${user_home}/store, 目录大体如下
```log
abort
checkpoint
commitlog
config      
consumequeue
index
lock       
```

### abort   
这个文件是 RocketMQ 用来判断程序是否正常关闭的一个标识文件。正常情况下，会在启动时创建，而关闭服务时删除。  
但是如果遇到一些服务器宕机，或者 kill -9 这样一些非正常关闭服务的情况，这个 abort 文件就不会删除，因此 RocketMQ 
就可以判断上一次服务是非正常关闭的，后续就会做一些数据恢复的操作。 

### checkpoint
数据存盘检查点。里面主要记录 commitlog 文件、ConsumeQueue 文件以及 IndexFile 文件最后一次刷盘的时间戳。

### commitlog
存储消息的元数据。所有消息都会顺序存入到 CommitLog 文件当中。CommitLog 由多个文件组成，每个文件固定大小 1G。以第一条消息的偏移量为文件名

### config
文件夹，里面都是一些 *.json 文件。  
这些文件是将 RocketMQ 的一些关键配置信息进行存盘保存。例如 Topic 配置、消费者组配置、消费者组消息偏移量 Offset 等等一些信息。

### consumequeue
存储消息在 CommitLog 的索引。一个 MessageQueue 一个文件，记录当前 MessageQueue 被哪些消费者组消费到了哪一条 CommitLog

### index
为消息查询提供了一种通过 key 或时间区间来查询消息的方法，这种通过 IndexFile 来查找消息的方法不影响发送与消费消息的主流程

### lock
类似 abort, 服务启动后，创建在当前目录的，表示当前目录有 Broker 在使用。


### 和消息存储相关的 3 个 文件

**CommitLog**

所有生产者发过来的消息，都会无差别的依次存储到 Commitlog 文件当中。这样的好处是可以**减少查找目标文件的时间**，让消息以最快的速度落盘。

CommitLog 文件结构
固定为 1 G, 但是里面存储的消息单元大小不固定。  
正因为消息的记录大小不固定，所以 RocketMQ 在每次存 CommitLog 文件时，都会去检查当前 CommitLog 文件空间是否足够，如果不够的话，就重新创建一个
CommitLog 文件。文件名为当前消息的偏移量。

**ConsumeQueue**
主要是加速消费者的消息索引
进入的第一层为当前 Broker 里面所有的 Topic 名称的文件夹。
进入某个 Topic 的文件夹，第二层是当前 Topic 里面对应的队列。
进入某个 队列 的文件夹, 第三层就是具体的文件, 大小为 6M --> 文件的内容 (每一项为: CommitLog Offset + 对应消息的 大小 + 对应消息的 Tag Hash 值) 每一项继续重复。

文件结构

文件 6M --> 固定 30w 个项，每一项 --> 
msgPhyOffset 8b, 消息在文件中的起始位置
msgSize 4b, 消息在文件中占用的长度
msgTagCode 8b 消息的 tag 的 Hash 值

**IndexFile**
消费端消费消息时, 指定从哪里开始消费，从最新，开头等情况开始消费。   
特殊情况: 从某个时间戳消费起，ConsumeQueue 不支持, 通过 IndexFile 开始消费。

IndexFile 文件主要是辅助消息检索。

RocketMQ 管理后台通过 MeessageId 或者 MessageKey 来检索消息时, ConsumerQueue 不支持。  

整个文件以时间戳命名

文件结构
indexHeader (固定40byte)  +  slot (固定 500W 个，每个固定 20byte, 只存最新 index 的值) +  index (最多 500W*4 个，每个固定 20byte)

500w * 4 可以认为 slot 出现冲突的可能性在 4 以内

https://blog.csdn.net/roykingw/article/details/120086520


## 过期文件删除

commitLog 和 consumeQueue.

CommitLog 文件和 ConsumeQueue 文件都是以偏移量命名，对于非当前写的文件，如果超过了一定的保留时间，那么这些文件都会被认为是过期
文件，随时可以删除。这个保留时间就是在 broker.conf 中配置的 fileReservedTime (单位小时, 过期文件多久时间没有消息写入, 就进行删除)。


注意，RocketMQ 判断文件是否过期的唯一标准就是非当前写文件的保留时间，而并不关心文件当中的消息是否被消费过。
所以，RocketMQ 的消息堆积也是有时间限度的。


RocketMQ 内部有一个定时任务，对文件进行扫描，并且触发文件删除的操作。
用户可以指定文件删除操作的执行时间。在 broker.conf 中 deleteWhen 属性指定。默认是凌晨四点。

另外，RocketMQ还会检查服务器的磁盘空间是否足够，如果磁盘空间的使用率达到一定的阈值，也会触发过期文件删除。
所以 RocketMQ 官方就特别建议，broker 的磁盘空间不要少于 4G。 (diskMaxUsedSpaceRatio, 默认 88)

## 高效文件写入

零拷贝 + 顺序写 + 刷盘机制


### 零拷贝

mmap sendfile 2 种零拷贝技术

一般情况:
File --> 内核态 --> 用户态
用户态 --> 内核态 --> 文件
这些都是需要 CPU 支持的

引入 DMA (直接存储器存储), 负责 IO 操作, CPU 管理 DMA 的权限即可, 减轻了 CPU 的压力。
数据复制过程中, DMA 还是需要借助数据总线, 当系统内的 IO 操作过多时，还是会占用过多的数据总线，造成总线冲突，最终还是会影响数据读写性能.
引入 Channel 通道, 是一个完全独立的处理器，专门负责 IO 操作。

而所谓的零拷贝技术，其实并不是不拷贝，而是要尽量减少 CPU 拷贝。


mmap 将文件读取到内核态缓冲区, 用户缓冲区是跟内核缓冲区共享一块映射数据的(可以理解为持有了内核态缓冲区中的一个 File 文件)
这样就减少了一次从内核态读取数据到用户态的 IO， mmap 减少了一次拷贝 (内核态拷贝到另一个内核态, 还是需要一次拷贝)
(缺点, 文件不能太大, 只能映射 1.5- 2G, 所以 RocketMQ 单个 commit log 1g)

原因: mmap的映射机制由于还是需要用户态保存文件的映射信息，数据复制的过程也需要用户态的参与，这其中的变数还是非常多的。


sendfile 去除了用户态,  引入了 offset 和 length
将文件的 fd, offset、length 等数据拷贝到内核态，这些数据在内核态传递给另一方 (Socket), Socket 通过这些数据读取文件
这样避免了文件在在各种态直接的拷贝
适用于大文件

### 刷盘

cat /proc/meminfo 查看当前系统的内存情况， 里面的 Cached 就是 Page Cache, 也就是常说的系统级缓存


将系统级缓存写入到磁盘, 就是刷盘 

同步刷盘 JVM 内存写入到 Page Cache, 然后强制刷入磁盘，每条消息都实时写入磁盘 (效果看起来是这样，但是实际中通过一些异步操作，提高了性能)
异步刷盘 JVM 内存写入到 Page Cache, 就结束, 后面定时器同步刷入磁盘, 有数据丢失情况


## 主从复制

主从节点消息的复制

同步复制
等Master和Slave都写入消息成功后才反馈给客户端写入成功的状态。
在同步复制下，如果Master节点故障，Slave上有全部的数据备份，这样容易恢复数据。但是同步复制会增大数据写入的延迟，降低系统的吞吐量。

异步复制
异步复制是只要master写入消息成功，就反馈给客户端写入成功的状态。然后再异步的将消息复制给Slave节点。
在异步复制下，系统拥有较低的延迟和较高的吞吐量。但是如果master节点故障，而有些数据没有完成复制，就会造成数据丢失

## 负载均衡

### 生产端负载均衡

Producer 发送消息时，默认会轮询目标 Topic 下的所有 MessageQueue，并采用递增取模的方式往不同的 MessageQueue 上发送消息，
以达到让消息平均落在不同的 queue 上的目的。而由于 MessageQueue 是分布在不同的 Broker 上的，所以消息也会发送到不同的 broker 上。

内部做了一些小优化, 上一次发送到的 Broker 有问题，会进行跳过


### 消费端负载均衡

集群模式

启动时, 会将 Topic 下的 MessageQueue 分配给对应消费者组里面的某个消费者进行消费， 一个 MessageQueue 一个消费者，但是一个消费者可以多个 Queue
里面有 7 种分配策略
AllocateMachineRoomNearby 同机房的Consumer和Broker优先分配
AllocateMessageQueueAveragely 平均分配   ---> 1, 2 分配给 A，3,4 分配给 B
AllocateMessageQueueAveragelyByCircle 轮询分配 1,2,3,4 分配完， 5678 分配
AllocateMessageQueueByConfig 不分配, 直接指定所有队列
AllocateMessageQueueByMachineRoom 按逻辑机房的概念进行分配
AllocateMessageQueueConsistentHash  hash
AllocateMessageQueueConsitentHashTest  哈希环

而每当实例的数量有变更，都会触发一次所有实例的负载均衡，这时候会按照
queue的数量和实例的数量平均分配queue给每个实例



广播模式
广播模式下，每一条消息都会投递给订阅了Topic的所有消费者实例，所以也就没
有消息分配这一说。而在实现上，就是在 Consumer 分配 Queue 时，所有 Consumer 都分到所有的 Queue。

广播模式实现的关键是将消费者的消费偏移量不再保存到broker当中，而是保存到客户端当中，由客户端自行维护自己的消费偏移量


## 消息重试

首先对于广播模式的消息， 是不存在消息重试的机制的，即消息消费失败后，不会再重新进行发送，而只是继续消费新的消息。 
而对于普通的消息，当消费者消费消息失败后，你可以通过设置返回状态达到消息重试的结果

重试的消息会进入一个 “%RETRY%”+ConsumeGroup 的队列中。  (一个消费者组消费多个 Topic, 都是共用这个重试队列)
然后RocketMQ默认允许每条消息最多重试16次， 时间有递增级别。

如果消息重试16次后仍然失败，消息将不再投递。转为进入死信队列。 重试次数可以在消费端进行配置。


重试消息在旧版本 MessageId 是一样的，但是在4.9.1版本中，每次重试MessageId都会重建

触发重试，可以在消费时
> 1. 返回 Action.ReconsumeLater
> 2. 返回 null
> 3. 抛出异常

## 死信队列

死信队列的名称是 %DLQ%+ConsumGroup, 默认的 perm 为 2， 禁读 (如果需要消费里面的消息，需要人工的将其设置为 6)

一个死信队列包含了这个ConsumeGroup里的所有死信消息，而不区分该消息属于哪个 Topic, 默认保存时间和正常的消息一样, 默认 3 天


## 消息幂等


行。RocketMQ 只能保证at least once 保证每条消息至少会被消费一次，所以需要由业务系统自行保证消息的幂等性。

msgId 官方不保证唯一性 （重试中会改变）, 所以官方建议通过自己的业务进行确定，比如 orderId
或者自己的唯一 MessageKey

发送时消息重复
当一条消息已被成功发送到服务端并完成持久化，此时出现了网络闪断或者客户
端宕机，导致服务端对客户端应答失败。 如果此时生产者意识到消息发送失败并
尝试再次发送消息，消费者后续会收到两条内容相同并且 Message ID 也相同的
消息。


投递时消息重复
消息消费的场景下，消息已投递到消费者并完成业务处理，当客户端给服务端反
馈应答的时候网络闪断。 为了保证消息至少被消费一次，消息队列 RocketMQ
的服务端将在网络恢复后再次尝试投递之前已被处理过的消息，消费者后续会收
到两条内容相同并且 Message ID 也相同的消息。


负载均衡时消息重复（包括但不限于网络抖动、Broker 重启以及订阅方应用重启）
当消息队列 RocketMQ 的 Broker 或客户端重启、扩容或缩容时，会触发
Rebalance，此时消费者可能会收到重复消息。

## Dledger 

RocketMQ中的Dledger集群主要包含两个功能：
1、从集群中选举产生master节点。
2、优化master节点往slave节点的消息同步机制。 2 阶段提交， uncommited, commited

# 第四章

## NameServer

NameServer 主要作用
> 维护Broker的服务地址并进行及时的更新
> 给Producer和Consumer提供服务获取Broker列表

https://www.cnblogs.com/vivotech/p/15323042.html


## Broker 

https://juejin.cn/post/7097497197729546248


BrokerConfig
NettyServerConfig
NettyClientConfig
MessageStoreConfig


## 客户端

RocketMQ 基于 Netty 保持客户端与服务端的长连接 Channel。只要 Channel 是稳定的, 通讯就稳定，通过监听 Channel 的状态，决定通讯方的状态，处理