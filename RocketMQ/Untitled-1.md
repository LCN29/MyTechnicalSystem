
消息一致性:
A -> B/C  一个成功, 一个失败


kafka 功能比较单一, 存在丢数据可能性
RabbitMQ 消息堆积影响性能


   
namesever, broker(主从)
broker 将自己的信息注册到 namesever, Producer/Consumer 从 namesever 获取 Broker 信息

发送消息方式
同步发送 等待 MQ 响应 (Producer 内部会尝试进行重试) producer.send(msg), 阻塞
异步发送  发送消息到 MQ, 同时携带一个回调函数, 后续就不管了, MQ 接受到这个消息后, 回调这个函数 producer.send(msg, new SendCallback())
单向发送, 只推送消息到 MQ, 不需要确认消失是否到 MQ, producer.sendOneway(msg)

接受消息
消费者主动到 Broker 拉消息 consumer.fetchSubscribeMessageQuery("Topic")
得到对应 tocpic 下的 MessageQueue, 死循环 MessageQueue, 获取里面的消息
consumer.pullBlockIfNotFound(MessageQueue, subExpression, offset, maxNums) （DefaultLitePullCOnsumer 替代过期方法）

Broker 主动推消息到消费者 (本质还是消费者拉)， consumer.registerMessageListener(new MessageListenerConcurrenly())





顺序消息 
producer.send(msg, new MessageQueueSelector()) 生产端指定消息推送到哪个 MessageQueue, 为顺序性提供条件
consumer.registerMessageListener(new MessageListenerQrderly())
优先把同一个队列中的消息获取完


广播消息
上面的情况，一个消息只会由一个消费者消费
同一个消息，可以多个消费者进行消费 consumer.setMessageModel(broadcast)

延迟消息
msg.setDelyTimeLevel(3), 先发到系统内部自己维护的一个 schedule_topic_xxx 的队列, 

批量消息
producer.send(List<Message>) 有消息大小限制
producer.send(ListSplitter<Message>) 内部提供的类，会自定计算大小

过滤消息
tag 的使用   
consumer.subscribt("topic", MessageSelector.bySql("TAGS is not null and a is not null and a between 0 and 3")) 指定消息的过滤条件，同时消息里面有 a 这个属性 (msg.putUserProperty("a", "11"))  SQL92 语法
