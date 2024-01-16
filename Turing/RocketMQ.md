
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



