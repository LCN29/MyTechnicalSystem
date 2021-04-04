# 02. synchronized - 偏向锁

前提说明:

>1. 下面的逻辑都是基于 OpenJDK 8 的 HotSpot 源码来, 并且 HotSpot 的字节码解析器进行梳理。虽然 HotSpot 在运行中使用的是用汇编书写的模板解释器, 但是基于 C++ 书写的字节码更容易阅读, 而且 2 者的实现逻辑基本都是一样的, 所以以字节码的方式进行讲解。
>2. 通过 synchronized 的代码块的方式进行讲解, 既 monitorenter 和 monitorexit 指令。而不是方法级的, 但是 2 者的锁逻辑都是类似的
>3. 为了避免枯燥, 会以伪代码的形式进行讲解, 同时会附上源代码的位置, 有兴趣的话, 可以进行了解。

那么开始了 !

## 2.1 偏向锁的简单介绍

在多线程的前提下, 并发是一个必然的问题。但是在大多数情况下锁是不存在竞争的, 而且总是由一个线程持有。基于这种线程, 就有了偏向锁的出现。 

偏向锁默认当前的锁是无竞争的, 它会偏向于第一个获得它的线程。在 synchronized 中, 在第一次获取这个锁的线程, 不会进行真正的锁对象（monitor）获取, 而是锁的对象的对象头从无锁状态转为偏向锁状态 (这个状态的转换, 在广义上讲是正确的, 但是将场景缩小到 HotSpot 的实现中, 就不能说是完全正确的)，并且把这个线程 Id 记录在对象头里, 后续只要这个锁没有被其他线程获取，那么这个线程就无需要进行同步，就能进入到同步代码块。

在 HotSpot 中对于 synchronized 的偏向实现, 如下

![Alt 'BasicLockChangeProcesses.png'](https://raw.githubusercontent.com/PictureRespository/Java/main/JUC/BasicLockChangeProcesses.png)

## 2.1 锁偏向(偏向锁的获取)

### 2.1.1 伪代码 - 01

先上一段伪代码 ([源码位置](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/9ce27f0a4683/src/share/vm/interpreter/bytecodeInterpreter.cpp#l1816))

```Java
CASE(_monitorenter): {

    //  获取当前锁对象
    Lock lock = get_lock();
    // 从栈中获取一个 锁记录
    LockRecord lock_record = get_lock_record_from_stack_or_stack_frame();

    // 获取不到 Lock Record, 执行执行这个指令
    if (lock_record == null) {
        // 重新执行
        re_execute();
        return;
    }

    // 下面执行锁逻辑

}
```

从上面的代码可以发现:
1. 无论是什么锁, 进来就会获取到一个空闲的 LockRecord (LockRecord 的 obj 属性为空的话, 就是空闲的)
2. LockRecord 可能分配在栈中, 也能存在栈帧中, 现在普遍的说法是分配在栈帧中。 这个不是猜测, 而是本人的一个疑问

**发现 1 分析**

在源代码中, getLockRecordFromStackOrStackFrame 的实现逻辑是这样的

```C++
// find a free monitor or one already allocated for this object
// if we find a matching object then we need a new monitor
// since this is recursive enter
BasicObjectLock* limit = istate->monitor_base();
BasicObjectLock* most_recent = (BasicObjectLock*) istate->stack_base();
BasicObjectLock* entry = NULL;
while (most_recent != limit ) {
    if (most_recent->obj() == NULL) entry = most_recent;
    else if (most_recent->obj() == lockee) break;
    most_recent++;
}
```

通过上面的备注和结合下面的伪代码逻辑 (找不到, 重新执行), 可以知道: **进入 monitorenter, 会在私有线程栈/栈帧找到一个最近并且空闲的区域, 创建一个Lock Record，里面属性为 null**,  
而不是在锁升级到轻量锁的时候才产生, 而且每一块 synchronized 代码块都需要一个空闲的 Lock Record。

**发现 2 产生的原因**
在 synchroized 方法锁的代码注释中出现了一句话 **Monitor not filled in frame manager any longer as this caused race condition with biased locking(Moitor 不再填充在帧管理器, 因为这将导致偏向锁的竞态条件)**, [源码位置](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/9ce27f0a4683/src/share/vm/interpreter/bytecodeInterpreter.cpp#l693), 这段备注是 JDK 8 才出现的, JDK 6, 7 没有

加上上面的方法返回值备注都是各种 "Stack", 才产生这个疑惑, Lock Record 在 JDK 8 后, 存储的位置从栈帧移到了栈中了?

后面在各种资料查询中, 只能找到下面的结论

1. 在官网的这篇编写于 2008 年介绍 synchronized 的[文章](https://wiki.openjdk.java.net/display/HotSpot/Synchronization)中, 明确说的了 Record Lock 是存放在栈帧的。

2. 在 Oracle 的官网中另一篇编写于 2006 年介绍 synchronized 的[文章](https://www.oracle.com/technetwork/java/biasedlocking-oopsla2006-wp-149958.pdf) 中对的 Lock Record 做了更详细的介绍:  
**在解释执行过程中, 栈帧中有一块区域用于存储 Lock Record, 这块区域会随着方法的执行变大或缩小**  
**在编译执行过程中, 是没有这块区域的, 而是以一个类似的方式存储在 register spill stack slots**  

官方文档中都是明确说了是在栈帧中, 所以网上说法的依据应该都是从这里来的。但是 2 篇文章都是 10 年以前编写的, 而上面的备注也是在 JDK 8 才出现的, 所以猜测会不会是在不同版本中, 有不同的实现了！

### 2.1.2 伪代码 - 02

```java
CASE(_monitorenter): {

    Lock lock = get_lock();
    Lock_record lock_record = get_lock_record_from_stack_or_stack_frame();

    if (lock_record == null) {
        re_execute();
        return;
    }

    // 设置当前的 Lock Record 的 obj 指向当前的锁
    lock_record.set_obj(lock);

    // 获取当前锁对象的 MarkWord
    MarkWord mark_word = lock.get_mark_word();

    // 获取当前的 markWord 中 no_hash 的值, 默认为 0
    int hash = mark_word.get_no_hash();
    // 偏向锁获取结果
    boolean bias_lock_get_result = false;

    // 当前对象的 MarkWord 是否为偏向锁状态, 既最后 3 位是否为 101
    if (mark_word.has_bias_pattern()) {
        // 尝试获取偏向锁逻辑
    }

    // 偏向锁获取失败, 进行锁升级
    if (!bias_lock_get_result) {
        // 轻量级锁逻辑
    }

    // 执行下一条指令
    execute_next_instruction();
}
```

从上面的逻辑走下去, 会发现一个有趣的状态: **无锁状态没法升级为偏向锁**。

这里涉及到一个偏向锁的机制: 无锁和偏向锁之间, 存在一个中间状态, 匿名偏向 (anonymously biased)。既锁升级的途径是从 匿名偏向 -> 偏向锁, 而不是无锁 -> 偏向锁 ！

匿名偏向: 锁对象已经是偏向锁的状态, 但是偏向的线程 Id 为 0, 表示未偏向任何线程。  
匿名偏向, 涉及一个虚拟机配置 -XX:BiasedLockingStartupDelay=xx 单位毫秒。

在 JVM 启动后的 BiasedLockingStartupDelay 秒内, 所以创建出来的对象和加载进来的都是**无锁状态**, 既这个时间内的使用的锁只能是**轻量级锁和重量级锁**。在 BiasedLockingStartupDelay 秒后, 创建出来的对象和加载进来的类默认都是**匿名偏向锁状态**。

如图:
![Alt 'AnonymouslyBiasedLock'](https://raw.githubusercontent.com/PictureRespository/Java/main/JUC/AnonymouslyBiasedLock.png)

上面的逻辑可以理通了！在程序运行中的情况, Java 对象绝大部分都是匿名偏向状态, 不存在无锁的情况！

如果需要关闭这种情况的话, 可以通过下面的几个参数进行设置:
> -XX:+UseHeavyMonitors  只使用重量锁
> -XX:+/-UseBiasedLocking  启用/关闭 偏向锁, JDK 6 后默认为启用状态
> -XX:BiasedLockingStartupDelay=0  关闭延迟开启偏向锁

### 2.1.3 伪代码 - 03

```java
CASE(_monitorenter): {

    Lock lock = get_lock();
    Lock_record lock_record = get_lock_record_from_stack_or_stack_frame();

    if (lock_record == null) {
        re_execute();
        return;
    }

    lock_record.set_obj(lock);
    MarkWord mark_word = lock.get_mark_word();
    int hash = mark_word.get_no_hash();
    boolean bias_lock_get_result = false;

    // 偏向锁状态处理
    if (mark_word.has_bias_pattern()) {
        
        // 当前偏向锁指向的线程等于当前线程和 锁里面的 epoch 等于当前锁对象的类的 PrototyHeader 属性的 epoch
        if (thread_id_in_basic_lock_equal_current_thread_id() 
            && epoch_in_basic_lock_equal_epoch_in_current_lock_class_prototype_header()) {
            // 当前线程已经持有了偏向锁, 不做任何事情, 结束
            biasLockGetResult = true;
        }

    }

    // 偏向锁获取失败, 进行锁升级
    if (!bias_lock_get_result) {
        // 轻量级锁逻辑
    }

    execute_next_instruction();
}
```

如果一个线程已经持有某个对象的锁了, 可以不需要重新获取锁了, 那么和判断一个对象是否持有锁了呢?
除了我们知道的锁对象头里面的线程 Id 等于当前的线程的 Id 外, 还需要满足另外一个条件, 对象头里面的 epoch 和 Class 对象内部维护的 prototype_header 的 epoch 一致 !

**首先 prototype_header 是什么**  
看一段 JVM 创建对象时的代码

```C++
// UseBiasedLocking 是否启用了偏向锁, 取值取决于 -XX:+/-UseBiasedLocking, 默认为 true
if (UseBiasedLocking) {
    result->set_mark(ik->prototype_header());
} else {
    result->set_mark(markOopDesc::prototype());
}
```

在启用了偏向锁的情况下, 创建出来的对象的 MarkWord 等于 Class prototype_header, 没有的话，就是自己内部的 prototype 的值
可以知道 prototype_header 就是对象 MarkWord 的初始模板 !

默认情况下, prototype_header 的值如下:

| 持有锁线程 Id | Epoch | 分代年龄 | 是否偏向锁标志| 锁标志位 |
| :-: | :-:  | :-: | :-: | :-: |
| 0 | 当前的 Epoch  | 0  | 1 |  01 |

所以上面的偏向锁延迟开启, 就是借助 prototype_header 实现的, 在开始时期, prototype_header 是无锁的, 那么创建出来的对象就是无锁的, 当时间到了, 替换为匿名偏向, 创建出来的对象就是匿名状态的。

**Epoch 是什么**

epoch 主要用于解决**重偏向**。首先重偏向主要用于处理锁实例的情况, 锁类的情况没法处理的。
重偏向, 从字面意思就能知道了, 某个实例锁当前是偏向了线程 A, 线程 B 获取这个实例锁时, 应该升级为轻量级锁变为偏向自己的偏向锁。简单的理解为乐观锁的版本号！版本号不一样了, 发生了变更！

机制的流程
1. 类 C 下有很多个实例被当做锁, 当前都是偏向锁, 类 C 和各个实例的 epoch 都是一样的
2. 这些锁突然出现了多次的锁升级, 每次偏向锁升级或降级时, 会在类 C 内部的变更记录 加 1
3. 线程 A 获取类 C 下一个偏向锁实例 I, 本来应该是会升级为轻量级锁的, 但是类 C 内部维护的变更记录达到了 X
4. 类 C 的 epoch + 1, 
5. 找到所有的类 C 的 LockRecord, 修改他们的 epoch = 类 C 的 epoch, 此时偏向锁内部的偏向的线程 Id 没有修改, 因为修改了会破坏锁的线程安全性 (在字节码解释器中是这样处理的, 实际跑中的, 效果没有这一步)
6. 当前线程 A 获取的实例 I, 从升级轻量级锁变为偏向锁, 偏向的线程为 A
7. 那么这些锁实例, 在线程下次尝试获取锁时, 当前实例的 epoch 和 class 的 epoch 不一致, 会先进入偏向锁, 不会立即升级为轻量级锁

例子
```java

List<Lock> list = new ArrayList<>();

new Thread(()->{

    for (int i = 0; i < 30; i++) {
        Lock lock =  new Lock();
        list.add(lock);
        // Lock 下有多个实例被偏向锁锁住
        synchronized(lock) {
            // 打印对象头信息, 都是偏向锁
            System.out.println(ClassLayout.parseInstance(lock).toPrintable()); 
        }
    }

}, "thread-01").start();

// 让上面的线程跑一下
Thead.sleep(5000L);

for (int i = 0; i < 30; i++) {

    Lock lock =  list.get(i);

    // Importance 这里打印的结果和上面的线程 thread-01 的完全一样
    System.out.println(ClassLayout.parseInstance(lock).toPrintable()); 

    // 下面的打印情况, 0-18都是轻量级锁, 19 后面都是偏向锁
    synchronized(lock) {
        // 打印对象头信息, 都是偏向锁
        System.out.println(ClassLayout.parseInstance(lock).toPrintable()); 
    }
}
```

看不太懂的话, 可以等到批量重偏向的时候, 过来回顾！


**在上面中, 如果要判断一个线程的 Id, Epoch 和锁对象存的线程 Id 和 Epoch 一样, 用了 2 个方法, 源码中也是这样的吗**  
答案是否的, 在源码中是通过计算出一个值, 比较预期值判断的，当前应该做什么分支的！

```C++
anticipated_bias_locking_value =
    (((uintptr_t)lockee->klass()->prototype_header() | thread_ident) ^ (uintptr_t)mark) & ~((uintptr_t) markOopDesc::age_mask_in_place);

// 线程 Id 和 epoch 都一样
if(anticipated_bias_locking_value == 0) {
}    
```

上面的代码的可以分为 4 步进行分析

第一步: **((uintptr_t)lockee->klass()->prototype_header() | thread_ident)** 将当前线程 Id (thread_ident) 和 class 的 prototype_header 相或。这样得到的值为: 当前的线程 Id + class 的 Epoch + 分代年龄 + 偏向锁标志 + 锁状态, 也就是 **23位的线程 Id + 2 位的 Epoch + 0000101**, class prototypeHeader 的年龄代默认为 0, 4 位

|prototype_header| 00000000|&nbsp;|00000000|&nbsp;|0000000X|&nbsp;|X0000101|
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|thread_ident|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXX0|&nbsp;|00000000|
| 或结果 |XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|X0000101|



第二步: **^ (uintptr_t)mark**, 将第一步的结果和当前锁的 markWord 进行异或操作(相等的位全部被置为 0)！那么我们能确定的结果只有最后 3 位为 000

| 第一步结果 |XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|X0000101|
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|markWord|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXX101|
| 或结果 |XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXX000|


第三步: **~((uintptr_t) markOopDesc::age_mask_in_place)** 只获取当前锁对象的年龄待,进行取反(1 变为 0, 0 变为 1), 那么可以知道结果为 25 个 1 + 4 个未知的年龄代 + 3 个 1

|prototype_header| 00000000|&nbsp;|00000000|&nbsp;|00000000|&nbsp;|0XXXX000|
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| 取反结果| 11111111|&nbsp;|11111111|&nbsp;|11111111|&nbsp;|1XXXX111|


第四步: **&** 将第 2 步和第 3 步的结果进行与操作(都为 1, 才为 1)


| 第二步结果 |XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXX000|
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| 第三步结果 |11111111|&nbsp;|11111111|&nbsp;|11111111|&nbsp;|1XXXX111|
| 与结果 |XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|XXXXXXXX|&nbsp;|X0000000|


分析 1: 
第二步的结果里面的年龄代值 = markWord 里面的年龄代的值
第三步的结果里面的年龄代值 = markWOrd 里面的年龄代的值取反
所以第三，第四步与的结果为年龄代的值为 0

分析 2:
假设当前线程的线程 Id, 既 thread_ident 和 MarkWord 里面的线程 Id 一样呢, 那么第二步的结果前 23 位将都为 0, 最终导致第四步的结果线程的 23 位都是 1 & 0 = 0

分析 3:
同 2, 当前 MarkWord 里面的 epoch 和 class 的 epoch 一样, 最终就是 0 & 1 = 0。

也就是当前线程的 Id 和 markWord 的线程 Id 一样, markWord 里面的 epoch 和 class 的 epoch 一样, 算出来的值为 0 


### 2.1.4 伪代码 - 04


```java
CASE(_monitorenter): {

    Lock lock = get_lock();
    Lock_record lock_record = get_lock_record_from_stack_or_stack_frame();

    if (lock_record == null) {
        re_execute();
        return;
    }

    lock_record.set_obj(lock);
    MarkWord mark_word = lock.get_mark_word();
    int hash = mark_word.get_no_hash();
    boolean bias_lock_get_result = false;

    // 偏向锁状态处理
    if (mark_word.has_bias_pattern()) {
        
        // 当前偏向锁指向的线程等于当前线程和 锁里面的 epoch 等于当前锁对象的类的 PrototyHeader 属性的 epoch
        if (thread_id_in_basic_lock_equal_current_thread_id() 
            && epoch_in_basic_lock_equal_epoch_in_current_lock_class_prototype_header()) {
            // 当前线程已经持有了偏向锁, 不做任何事情, 结束
            biasLockGetResult = true;
        }

    }

    // 偏向锁获取失败, 进行锁升级
    if (!bias_lock_get_result) {
        // 轻量级锁逻辑
    }

    execute_next_instruction();
}
```


