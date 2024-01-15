

官网: https://rocketmq.apache.org/docs/


Kafka 的弊端
topic 太多, 消息吞吐性能。

一个 topic 下有多个 Partition, 文件按照 Partition 组织。
topic 增加 --> Partition 增加 --> 文件增加 --> 写性能下降, 写大量的索引。

日志场景下, topic 不需要很多。


RocketMQ   
适用场景 topic 很多, 业务场景多的情况。