# 02. synchronized - 偏向锁

前提说明:

>1. 下面的逻辑都是基于 OpenJDK 8 的字节码解析器进行梳理。虽然 HotSpot 在运行中使用的是用汇编书写的模板解释器, 但是基于 C++ 书写的字节码更容易阅读, 而且 2 者的实现逻辑基本都是一样的, 所以以字节码的方式进行讲解。
>2. 通过 synchronized 的代码块的方式进行讲解, 既 monitorenter 和 monitorexit 指令。而不是方法级的, 但是 2 者的锁逻辑都是类似的
>3. 为了避免枯燥, 会以伪代码的形式进行讲解, 同时会附上源代码的位置, 有兴趣的话, 可以进行了解。

那么开始了 !

## 2.1 偏向锁的简单介绍

在多线程的前提下, 并发是一个必然的问题。但是在大多数情况下锁是不存在竞争的, 而且总是由一个线程持有。基于这种情况, 就有了偏向锁的出现。 

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

在源代码中, get_lock_record_from_stack_or_stack_frame 的实现逻辑是这样的

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

    // 设置当前的 Lock Record 的 obj 指向当前的锁, 从空闲状态变为非空闲
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

匿名偏向: 锁对象已经是偏向锁的状态, MarkWord 最后 3 位已经是 101,  但是偏向的线程 Id 为 0, 表示未偏向任何线程。  

这里涉及到一个偏向锁的机制: 无锁和偏向锁之间, 存在一个中间状态, 匿名偏向 (anonymously biased)。既锁升级的途径是从 匿名偏向 -> 偏向锁, 而不是无锁 -> 偏向锁 ！


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
        
        // 当前偏向锁指向的线程等于当前线程和 锁里面的 epoch 等于当前锁对象的类的 prototype_header 属性的 epoch
        if (thread_id_in_basic_lock_equal_current_thread_id() 
            && epoch_in_basic_lock_equal_epoch_in_current_lock_class_prototype_header()) {
            // 当前线程已经持有了偏向锁, 不做任何事情, 结束
            bias_lock_get_result = true;
        }

    }

    // 偏向锁获取失败, 进行锁升级
    if (!bias_lock_get_result) {
        // 轻量级锁逻辑
    }

    execute_next_instruction();
}
```

如果一个线程已经持有某个对象的锁了, 可以不需要重新获取锁了, 那么如何判断一个对象是否持有锁了呢?
除了我们知道的锁对象头里面的线程 Id 等于当前的线程的 Id 外, 还需要满足另外一个条件, 对象头里面的 epoch 和 Class 对象内部维护的 prototype_header 的 epoch 一致 !

**首先 prototype_header 是什么**  
先看一段 JVM 创建对象时的代码

```C++
// UseBiasedLocking 是否启用了偏向锁, 取值取决于 -XX:+/-UseBiasedLocking, 默认为 true
if (UseBiasedLocking) {
    result->set_mark(ik->prototype_header());
} else {
    result->set_mark(markOopDesc::prototype());
}
```

在启用了偏向锁的情况下, 创建出来的对象的 MarkWord 等于 Class (ik) 的 prototype_header, 没有的话，就是自己内部的 prototype 的值
可以知道 prototype_header 就是一套维护在 Class 上用于初始对象 MarkWord 的初始模板 !

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
Thread.sleep(5000L);

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
        
        if (thread_id_in_basic_lock_equal_current_thread_id() 
            && epoch_in_basic_lock_equal_epoch_in_current_lock_class_prototype_header()) {

            // 情景 1: 锁当前就是偏向的当前线程, 不做任何事情了
            // 默认获取锁成功
            bias_lock_get_result = true;
        } else if (biased_mode_in_class_had_close()) {

            // 情景 2:  当前 class 的偏向锁模式关闭了, 但是 markWord 的还未关闭
            // 先通过 CAS 进行锁的撤销, 无论 CAS 是否成功, 都进入锁升级

            MarkWord header = lock.get_class().get_prototype_header();
            if (hash != mark_word.get_no_hash()) {
                header.copy_set_hash(hash);
            }   
            // TODO 源码这样, 不会导致年龄代信息丢失? 

            // 先通过 CAS 将头部
            if (CAS(header, lock.get_mark_word(), mark_word) == mark_word) {
                // 源码中，做了一些锁操作的分析统计, 不涉及锁逻辑
            }  

            // 无论是否执行成功, 都进入锁升级阶段

        } else if (epoch_in_class_instance_no_equal_epoch_in_class()) {
            // 情景 3: 有别的线程触发了批量重偏向, 导致当前锁实例的 epoch 和 class 身上的 epoch 版本不一致了
            // epoch 先尝试重偏向, 重偏向成功获取锁, 失败进入锁竞争

            MarkWord new_header = lock.get_class().get_prototype_header() | current_thead_Id();
            if (hash != markWord.get_no_hash()) {
                new_header.copy_set_hash(hash);
            } 
            // TODO 源码这样, 不会导致年龄代信息丢失? 

            if(CAS(new_header, lock.get_mark_word(), mark_word) == mark_word) {
                // 源码中，做了一些锁操作的分析统计, 不涉及锁逻辑
            } else {
                // 重偏向失败, 代表存在多线程竞争, 则调用 InterpreterRuntime.monitorenter 方法进行锁竞争
                InterpreterRuntime.monitorenter(current_thread(), lock_record);
            }
            bias_lock_get_result = true;
        } else {
            // 情景 4: 走到这里的, 只有锁偏向的线程和当前的线程不一样了, 那么有可能是匿名偏向, 要么线程 Id 完成不一样
            
            // 创建一个和当前锁对象的 MarkWord 一样但是没有线程 ID 的 MarkWord
            MarkWord header = create_mark_word_without_thread_id(mark_word);
            if (hash != mark_word.get_hash()) {
                header.copy_set_hash(hash);
            }

            // 让上面创建的 markWord 偏向当前线程
            MarkWord new_header = header | current_thead_id();

            if(CAS(new_header, lock.get_mark_word(), header) == header) {
                // 源码中，做了一些锁操作的分析统计, 不涉及锁逻辑
            } else {
                // 重偏向失败, 代表存在多线程竞争, 则调用 InterpreterRuntime.monitorenter 方法进行锁升级
                InterpreterRuntime.monitorenter(current_thread(), lock_record);
            }
            bias_lock_get_result = true;
        }
    }

    // 偏向锁获取失败, 进行锁升级
    if (!bias_lock_get_result) {
        // 轻量级锁逻辑
    }

    execute_next_instruction();
}
```

上面的伪代码中

**CAS**  
CAS(想要更新成的值, 要修改的对象, 预期的值(更新前的值)), 返回值为当前时间的原值

**4 种场景**  
上面的 4 种情景应该都不难理解。唯一要说明的, 应该只有第 4 种情况, 变量 header 里面是没有线程 Id 的!
如果 mark_word 是匿名偏向, 那么 CAS 才有可能失败 (锁竞争), 但是已经偏向其他线程的情况下, 100% 是会失败, 进入锁竞争


## 2.2 偏向锁撤销

撤销: 获取偏向锁的过程因为不满足条件导致要将锁对象改为非偏向锁状态。最简单的场景: 第一次调用了锁对象的 hashCode() 方法(没有重写), System.identityHashCode() 方法。处于偏向锁的锁对象, 会撤销为无锁状态。

在不重写类的 hashCode() 方法的条件下, 在 Java 语言里面一个对象如果计算过哈希码, 需要维持不变, 否则很多依赖对象哈希码的 API 都可能存 在出错风险 (HashMap 里面就是通过 hashcode 确定位置)。一般情况下, 计算过一次的哈希码会存储在实例的对象头中 (无锁状态)! 但是在偏向锁的情况下, 大部分的空间都用于存储线程 Id。所以为了存储哈希码, JVM 会根据当前对象处于什么锁状态, 是否是在锁内计算哈希码等情况, 对锁撤销, 或升级, 具体的情况, 具体分析。

在上面的锁偏向的, 情况 3, 4 进入锁竞争的链路为

interpreterRuntime.cpp -> InterpreterRuntime::monitorenter   
跳转到 synchronizer.cpp -> fast_enter   
再跳转到 biasedLocking.cpp -> revoke_and_rebias, 源码[地址](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/91b61f678a19/src/share/vm/runtime/biasedLocking.cpp#l554)

最终会走到 revoke_and_rebias() 方法, 进行是否锁撤销的逻辑判断

### 2.2.1 伪代码 - 01

```java
/**
 * 锁撤销/锁偏向
 * 
 * @param obj 一个包装类, 包装了锁对象和当前线程
 * @param attempt_rebias 是否允许重偏向, 经过上面的链路, 到了这里为 true
 */
Condition revoke_and_rebias(Handle obj, boolean attempt_rebias) {

    // 获取锁对象的 markWord
    MarkWord mark_word = obj.get_lock().get_mark_word();

    // 1. 可以不更新锁撤销次数统计的情况


    // 2. 更新撤销撤销次数, 根据撤销次数判断走什么情况


    // 3. 根据第 2 步的返回值, 进行分支处理
    // 3.1 当前锁对象不是偏向锁了, 不处理
    // 3.2 当前锁对象撤销次数达到了 BiasedLockingBulkRevokeThreshold 次, 默认为 40, 进行批量撤销
    // 3.3 当前锁对象撤销次数达到了 BiasedLockingBulkRebiasThreshold 次, 默认为 20, 进行批量重偏向
    // 3.4 当前锁对象撤销次数不在上面 3.2, 3.3 步的临界点, 只需要撤销当前锁对象
```

![Alt 'BiasedLockChangeTime'](https://raw.githubusercontent.com/PictureRespository/Java/main/JUC/BiasedLockChangeTime.png)

### 2.2.2 伪代码 - 02

```java
Condition revoke_and_rebias(Handle obj, boolean attempt_rebias) {

    MarkWord mark_word = obj.get_lock().get_mark_word();

    // 1. 可以不更新锁撤销次数统计的情况

    // 2. 更新撤销撤销次数, 根据撤销次数判断走什么情况
    HeuristicsResult heuristics = update_heuristics(obj.get_lock(), attempt_rebias);

    // 3. 根据第 2 步的返回值, 进行分支处理
}

/**
 * @param lock 当前的锁实例
 * @param allow_rebias 是否可以重偏向
 */
static HeuristicsResult update_heuristics(Lock lock, boolean allow_rebias) {

    MarkWord mark = lock.get_mark_word();

    //如果不是偏向模式直接返回
    if (!mark.has_bias_pattern()) {
        // 不需要做任何事情
        return HR_NOT_BIASED;
    }

    // 获取当前类
    Class class = lock.get_class();
    // 当前时间
    long cur_time = get_current_time();
    // 上次批量撤销的时间    
    long last_bulk_revocation_time = class.get_last_biased_lock_bulk_revocation_time();
    // 当前撤销的次数
    int revocation_count = class.get_biased_lock_revocation_count();

     if ((revocation_count >= BiasedLockingBulkRebiasThreshold) 
        && (revocation_count <  BiasedLockingBulkRevokeThreshold) 
        && (last_bulk_revocation_time != 0) 
        && (cur_time - last_bulk_revocation_time >= BiasedLockingDecayTime)) {
            
        // 偏向锁撤销次数降级    
        class.set_biased_lock_revocation_count(0);
        revocation_count = 0;     
    }

    // 没达到重批量撤销的上限, 原子自增
    if (revocation_count <= BiasedLockingBulkRevokeThreshold) {
        revocation_count = class.atomic_incr_biased_lock_revocation_count();
    }

    // 批量撤销
    if (revocation_count == BiasedLockingBulkRevokeThreshold) {
        return HR_BULK_REVOKE;
    }

    // 批量重偏向
    if (revocation_count == BiasedLockingBulkRebiasThreshold) {
        return HR_BULK_REBIAS;
    }

    // 当个撤销
    return HR_SINGLE_REVOKE;
}
```

update_heuristics 方法决定了当前线程获取偏向锁失败时, 如何处理！  
实现方式:  
>1. 锁对象对应的 class 内部维护了一个变量  biased_lock_revocation_count, 默认值为 0, 每次发生了锁撤销时, 次数 + 1
>2. 下次, 线程进来了, 会根据当前这个 biased_lock_revocation_count 的次数, 进行不同的操作
>3. biased_lock_revocation_count 次数刚好为 BiasedLockingBulkRevokeThreshold, 进行批量撤销
>4. biased_lock_revocation_count 次数刚好为 BiasedLockingBulkRebiasThreshold, 进行批量重偏向
>5. 不在 BiasedLockingBulkRevokeThreshold 和 BiasedLockingBulkRebiasThreshold 2 个临界点, 只对这个锁对象进行撤销
>6. class 还维护了另外一个变量 last_biased_lock_bulk_revocation_time, 上次更新批量撤销的时间, 这里是一个坑, 虽然叫做撤销的时间, 实际上是上次重偏向的时间
>7. class 的对象已经进行过一次批量重偏向了, 正常是继续进行撤销, 达到了批量撤销的次数时, 进行撤销
>8. 但是在上一次批量重偏向到达到批量撤销次数的过程中, 只要某一次的时间和上一次批量重偏向的时间大于了 BiasedLockingDecayTime 了, 那么就重置撤销次数, 又能进行批量重偏向了

大体如图:
![Alt 'BiasedLockingDecayTime'](https://raw.githubusercontent.com/PictureRespository/Java/main/JUC/BiasedLockingDecayTime.png)

里面涉及到的几个 JVM 参数配置
>1. -XX:BiasedLockingBulkRevokeThreshold= xx, 默认值为 40, 进行批量撤销的次数
>2. -XX:BiasedLockingBulkRebiasThreshold=xx, 默认值为 20, 进行批量重偏向的次数
>3. -XX:BiasedLockingDecayTime=xx,  默认值 25000, 单位毫秒, 批量重偏向次数重置时间


### 2.2.3 伪代码 - 03

```java

Condition revoke_and_rebias(Handle obj, boolean attempt_rebias) {

    MarkWord mark_word = obj.get_lock().get_mark_word();

    // 1. 可以不更新锁撤销次数统计的情况
    if (mark_word.is_biased_anonymously() && !attempt_rebias) {

        // 当前锁对象是匿名偏向锁, 且 attempt_rebias==false 会走到这里, 如锁对象的 hashcode 方法被调用会出现这种情况, 需要撤销偏向锁

        MarkWord biased_value = mark_word;

        // 匿名偏向锁的重偏向是一个很高效的过程
        // 这种情况, 可以不更新锁偏向次数统计, 因为这样可能会造成大量的不是必要的锁批量重偏向, 这是一个昂贵的操作

        // 构建一个无锁状态的 MarkWord
        MarkWord unbiased_prototype = mark_word.get_prototype().set_age(mark_word.get_age());

        // 通过 CAS 将当前锁替换为无锁状态
        MarkWord res_mark = CAS(unbiased_prototype, obj.get_mark_word(), biased_value);
        // 替换成功, 结束, 失败了, 说明有别的线程在进行竞争, 当前竞争失败, 走下面的处理
        if (res_mark == biased_value) {
            return BIAS_REVOKED;
        }
    } else if (mark_word.has_bias_pattern()) {

        // 当前是偏向锁状态
        Class k = obj.get_lock().get_class();
        // 获取当前类的初始 MarkWord
        MarkWord prototype_header = k.get_prototype_header();

        // 并发场景的兼容, 前面已经有线程导致锁升级或撤销了

        // 虽然当前锁对象的允许重偏向, 但是 class 已经关闭了偏向模式, 但是线程在其撤销的过程, 已经跑到了这里, 拦下来, 可以不走下去了
        if (!prototype_header.has_bias_pattern()) {

            // 当前锁对象的 Class 已经发生了批量撤销了, 但是锁对象当前是一个有误差的偏向锁状态
            // 这种情况去更新锁偏向次数统计是没有什么意义的, 我们只需要通过 CAS 更新其锁为当前 Class 的锁状态就行了

            // CAS 更新失败了, 说明有另外一个线程已经撤销了这个对象的锁偏向了,
            // 所以, 无论 CAS 更新成功失败, 都无所谓, 直接返回, 让调用方继续执行即可了
     
            MarkWord res_mark = CAS(prototype_header, mark_word, mark_word);
            return BIAS_REVOKED;

        } else if (prototype_header.get_epoch() != mark_word.get_epoch()){
            // epoch 不一样了

            // 在这次重偏向的过程中, 锁对象的 epoch 已经过期了, 说明这个实际上是未偏向的
            // 出现这种情况 2 个线程, 线程 A 已经先完成了锁的批量重偏向, 线程 B 这时执行到了上面的 else if 
            // 对于这种情况的话, 我们可以直接通过 CAS 更新这个锁对象的 MarkWord 为新的重偏向状态, 
            // 如果 CAS 更新失败了, 那么就说明此处还有线程在和线程 B 竞争这个锁对象, 那么锁撤销走起

            if (attempt_rebias) {

                // 允许重偏向的处理
                MarkWord biased_value = mark_word;

                // 获取一个新的偏向锁 MarkWord
                MarkWord rebiased_prototype = create_rebiased_mark_word(current_thread_id(), mark_word);
                
                MarkWord res_mark = CAS(rebiased_prototype, mark_word, mark_word);
                if (res_mark == biased_value) {
                    return BIAS_REVOKED_AND_REBIASED;
                }
            } else {

                // 不允许重偏向的话, 通过 CAS 将这个锁对象更新为无锁状态,
                // 更新失败的话, 走下面的撤销过程

                // 允许重偏向的处理
                MarkWord biased_value = mark_word;

                // 构建一个无锁状态的 MarkWord
                MarkWord unbiased_prototype = create_unbiased_mark_word(current_thread_id(), mark_word);
                
                 // 通过 CAS 将当前锁替换为无锁状态
                MarkWord resMark = CAS(unbiased_prototype, mark_word, biased_value);

                // 替换成功, 结束
                if (resMark == biased_value) {
                    return BIAS_REVOKED;
                }

            }
        }
    }


    // 2. 更新撤销撤销次数, 根据撤销次数判断走什么情况
    HeuristicsResult heuristics = update_heuristics(obj.get_lock(), attempt_rebias);

    // 3. 根据第 2 步的返回值, 进行分支处理
}
```

进行撤销的情景中, 存在可以不更新撤销统计的情况
>1. 匿名偏向
>2. 并发导致的, 偏向模式已被关闭, 当时当前对象还未知道
>3. 并发导致的, 批量重偏向，epoch 更新了, 但是当前对象还未知道


### 2.2.4 伪代码 - 04

```java
Condition revoke_and_rebias(Handle obj, boolean attempt_rebias) {

    MarkWord mark_word = obj.get_lock().get_mark_word();

    // 1. 可以不更新锁撤销次数统计的情况
    // 省略

    // 2. 更新撤销撤销次数, 根据撤销次数判断走什么情况
    HeuristicsResult heuristics = update_heuristics(mark_word, attempt_rebias);

    // 3. 根据第 2 步的返回值, 进行分支处理

    // 不是偏向锁, 直接结束
    if (heuristics == HR_NOT_BIASED) {
        return NOT_BIASED;
    } else if (heuristics == HR_SINGLE_REVOKE) {

        // 单个锁对象的撤销

        MarkWord prototype_header = mark_word.get_class().get_prototype_header();
        if (mark_word.get_thread_id() == current_thread_id() 
            && prototype_header.get_epoch() == mark_word.get_epoch()) {
            // 当前锁对象撤销
            Condition cond = revoke_bias(obj.get_lock(), false, false, current_thread());
            return cond;
        } else {

            // 需要撤销的锁对象偏向的其他线程, 那么需要进入安全点, 才能进行操作
            // 下面代码最终会在 VM 线程中的 safepoint 调用 revoke_bias 方法

            // C++ 里面的语法大概是这个意思
            VM_RevokeBias revke = new VM_RevokeBias(obj.get_Lock(), current_thread());
            VMThread.execute(revke);
            return revke.status_code();
        }
    }
    // 暂时省略
}
```    

上面的代码很简单, 如果需要撤销的锁对象和当前的线程 Id 一样, 那么直接执行锁撤销。不是的话, 向 VM Thread 注册一个锁撤销的事件, 得到安全点进行出现！

在 JVM 中有个专门的 VM Thread, 该线程会源源不断的从 VMOperationQueue 中取出 VMOperation 请求, 比如 GC 请求指令的。

### 2.2.5 伪代码 - 05

下面就是锁撤销的具体逻辑了

```java
/**
 * 锁撤销
 * @param 锁对象  
 * @param allow_rebias 是否允许重偏向 这里为 fasle
 * @param is_bulk 是否为批量操作
 * @param requesting_thread 请求撤销锁的线程
 */
static Condition revoke_bias(Lock lock, boolean allow_rebias, boolean is_bulk, JavaThread requesting_thread) {

    MarkWord mark = lock.get_mark_word();
    // 如果没有开启偏向模式，则直接结束
    if (!mark.has_bias_pattern()) {
        return BiasedLocking::NOT_BIASED;
    }

    // 创建一个匿名偏向的 MarkWord
    MarkWord biased_prototype = create_biased_mark_word(mark.get_age());
    // 创建出一个无锁的 MarkWord
    MarkWord unbiased_prototype = create_unbiased_mark_word(mark.get_age());

    // 从 markWord 获取当前偏向的线程
    JavaThread biased_thread = mark.get_biased_thread();

    if (biased_thread == null) {
        // mark 没有线程, 既是匿名偏向锁
        // 如果不允许偏向锁, 降为无锁
        if (!allow_rebias) {
            lock.set_mark(unbiased_prototype);
        }

        return BiasedLocking::BIAS_REVOKED;
    }

    boolean thread_is_alive = false;
    // 请求撤销锁的线程和当前偏向的线程一样
    if (requesting_thread == biased_thread) {
        thread_is_alive = true;
    } else {
        // 遍历当前所有的线程, 查看有相同的不
        for (JavaThread cur_thread = Threads.first(); cur_thread != null; cur_thread = cur_thread.next()) {
            if (cur_thread == biased_thread) {
                thread_is_alive = true;
                break;
            }
        }
    }

    // 线程不存在了
    if (!thread_is_alive) {
        // 允许重偏向的话, 撤销为匿名偏向
        if (allow_rebias) {
            lock.set_mark(biased_prototype);
        } else {
            // 不允许偏向了, 撤销为无锁
            lock.set_mark(unbiased_prototype);
        }
    }

    // 找到当前偏向锁对应的线程上面所有的 Lock Record
    // 这些 LockRecord 按照新放入到旧就放入的顺序排好了
    List<LockRecord> cached_monitor_info = get_or_compute_monitor_info(biased_thread);
    LockRecord highest_lock = null;

    for (int i = 0; i < cached_monitor_info.size(); i++) {
        LockRecord lock_record = cached_monitor_info.get(i);
        // 如果能找到对应的 Lock Record 说明偏向的线程还在执行同步代码块中的代码
        if (lock_record.get_obj() == lock) {
            highest_lock = lock_record;
            // 设置为 null ! 在这里虽然对应偏向锁没什么意思, 但是在轻量级锁的时候有用
            highest_lock.set_displaced_header(null);
        }
    }

    if (highest_lock != null) {
   
        // 将最外层的 lock record 设置为无锁
        highest_lock.set_displaced_header(unbiased_prototype);
    } else {
        // 找不到匹配的, 将当前的 mark_word 设置为无锁或匿名状态即可了
        if (allow_rebias) {
            lock.set_mark(biased_prototype);
        } else {
            // 无锁
            obj.set_mark(unbiased_prototype);
        }
    }
    return BiasedLocking::BIAS_REVOKED;
}
```

上面就是锁撤销的逻辑。当锁对象偏向的线程还在同步块中时, 就会变成我们常说的锁升级。
当对应的偏向的线程不在代码块内的话或者不在了, 撤销就是变为匿名偏向或无锁


## 2.3 批量重偏向

批量重偏向的作用在上面已经说过了!
一个线程创建了大量对象并执行了初始的同步操作, 之后在另一个线程中将这些对象作为锁进行之后的操作。这种情况下，会导致大量的偏向锁撤销操作, 所以 JVM 通过 class 的 epoch 变动, 让线程在获取锁的时候, 有机会进行重偏向, 偏向应该是对的线程, 而不是一直的撤销操作

### 2.3.1 伪代码 - 01

批量重偏向的入口和撤销的入口是一样的, 都是在通过 update_heuristics() 方法的返回值, 进入不同的操作

```java
static Condition revoke_bias(Lock lock, bool allow_rebias, bool is_bulk, JavaThread requesting_thread) {

    // 省略

    // 2. 更新撤销撤销次数, 根据撤销次数判断走什么情况
    HeuristicsResult heuristics = update_heuristics(mark_word, attempt_rebias);

    if (heuristics == HR_NOT_BIASED) {
        return NOT_BIASED;
    } else if (heuristics == HR_SINGLE_REVOKE) {
        // 省略
    }

    boolean is_bulk_rebias = heuristics == HR_BULK_REBIAS;

    // 向 VM 注册一个事件, 等待安全点执行, 最终执行了 bulk_revoke_or_rebias_at_safepoint 方法
    VM_RevokeBias bulk_revoke = new VM_RevokeBias(mark_word, current_thread(), is_bulk_rebias, attempt_rebias);
    VMThread.execute(bulk_revoke);
    return bulk_revoke.status_code();
}

/**
 * 在安全点, 批量重偏向/撤销
 * @param lock 锁对象
 * @param bulk_rebias 是否为批量重偏向，此处为 true 
 * @param attempt_rebias_of_object 对锁对象进行重偏向 此处为 true
 * @param requesting_thread 发出批量重偏向/撤销的线程
 */
static Condition bulk_revoke_or_rebias_at_safepoint(Lock lock, boolean bulk_rebias, boolean attempt_rebias_of_object, JavaThread requesting_thread) {

    // 更新 class 身上的上次批量操作的时间为当前时间
    long cur_time = System.currentTimeMillis();
    lock.get_class().set_last_biased_lock_bule_revocation_time(cur_time);

    Class k_o = lock.get_class();
    Class klass = k_o;

    if(bulk_rebias) {
        // 批量更新操作

        // 还是偏向锁状态
        if (klass.get_prototype_header().has_bias_pattern()) {
            // 当前的 epoch
            int prev_epoch = klass.prototype_header().bias_epoch();
            // 自增
            klass.prototype_header().incr_bias_epoch();
            // 最新的 epoch
            int cur_epoch = klass.prototype_header().bias_epoch();

            // 遍历当前所有的线程, 将每个线程里面的 Lock Record 的 epoch 都设置为 cur_epoch
            for (JavaThread cur_thread = Threads.first(); cur_thread != null; cur_thread = cur_thread.next()) {

                // 找到当前线程上面所有的 Lock Record
                // 这些 LockRecord 按照新放入到旧就放入的顺序排好了
                List<LockRecord> cached_monitor_info = get_or_compute_monitor_info(cur_thread);
                for (int i = 0; i < cached_monitor_info.size(); i++) {

                    LockRecord lock_record = cached_monitor_info.get(i);
                    Lock temp_lock = lock_record.get_obj();
                    MarkWord temp_mark = temp_lock.get_mark_word();
                    // Lock Record 锁的对象的 class 和当前的 class 一样, 锁对象还是偏向锁模式
                    if ((temp_lock.get_klass() == k_o) && temp_mark.has_bias_pattern()) {
                        temp_mark.set_bias_epoch(cur_epoch);
                    }
                }
            }
        }

        // 请求的线程, 进入撤销状态
        revoke_bias(lock, (attempt_rebias_of_object && klass.get_prototype_header().has_bias_pattern()), true, requesting_thread);

    } else {
        // 批量撤销操作
    }

    // 将当前锁对象偏向请求的线程
    BiasedLocking::Condition status_code = BiasedLocking::BIAS_REVOKED;
    if (attempt_rebias_of_object 
      && lock.get_mark().has_bias_pattern() 
      && klass.prototype_header().has_bias_pattern()) {

        // 构造一个偏向请求线程的 markWord
        MarkWord new_mark = create_mark(requesting_thread, lock.get_mark().get_age(), klass.get_prototype_header().bias_epoch())
        // 更新当前锁对象的mark word
        lock.set_mark_word(new_mark);
        status_code = BiasedLocking::BIAS_REVOKED_AND_REBIASED;
    }
    return status_code;
}
```

批量重偏向的操作很简单
1. 就是把锁对象的 Class 身上维护的 epoch 加 1
2. 把当前所有线程里面的 Lock Record 的 epoch 也加 1 (在测试的时候没有发现这个效果)
3. 请求的线程进行单独的撤销操作

猜测是字节码解释器和模板解释器这里存在差异, 个人认为不更新 epoch, 在 monitor_enter 的入口, 线程发现当前锁对象的 epoch 和 class 不一样的, 同样可以进行锁的重偏向。进行了修改了, 反而会走到锁撤销的逻辑

## 2.4 批量撤销

存在明显多线程竞争的场景下使用偏向锁是不合适的, 这时候会撤销所有锁对象的偏向模式

### 2.3.1 伪代码 - 01

代码入口依旧是在 **revoke_bias()** 方法, 再调到 **bulk_revoke_or_rebias_at_safepoint()**

```java
static Condition bulk_revoke_or_rebias_at_safepoint(Lock lock, boolean bulk_rebias, boolean attempt_rebias_of_object, JavaThread requesting_thread) {

    // 更新 class 身上的上次批量操作的时间为当前时间
    long cur_time = System.currentTimeMillis();
    lock.get_class().set_last_biased_lock_bule_revocation_time(cur_time);

    Class k_o = mark_work.get_class();
    Class klass = k_o;

    if(bul_rebias) {
        // 批量重偏向
    } else {
        
        // 将类中的偏向标记关闭，lock.prototype() 返回的是一个关闭偏向模式的 markWord
        klass.set_prototype_header(lock.prototype());

        // 遍历当前所有的线程, 将每个线程里面的 Lock Record 的 epoch 都设置为 cur_epoch
        for (JavaThread cur_thread = Threads.first(); cur_thread != null; cur_thread = cur_thread.next()){
            // 找到当前线程上面所有的 Lock Record
            // 这些 LockRecord 按照新放入到旧就放入的顺序排好了
            List<LockRecord> cached_monitor_info = get_or_compute_monitor_info(cur_thread);
            for (int i = 0; i < cached_monitor_info.size(); i++) {

                LockRecord = lock_record = cached_monitor_info.get(i);
                Lock temp_lock = lock_record.get_obj();
                MarkWord temp_mark = temp_lock.get_mark_word();

                // Lock Record 锁的对象的 class 和当前的 class 一样, 锁对象还是偏向锁模式
                if (temp_lock.get_lock() == k_o) && temp_mark.has_bias_pattern()) {
                    // 逐个撤销
                    revoke_bias(lock, false, true, requesting_thread);
                }

            }
        }

        // 撤销当前锁对象的偏向模式
        revoke_bias(o, false, true, requesting_thread);
    }

    // 将当前锁对象偏向请求的线程
    BiasedLocking::Condition status_code = BiasedLocking::BIAS_REVOKED;
    
    // 批量重偏向, 下面的判断不会执行
    if (attempt_rebias_of_object 
      && lock.get_mark().has_bias_pattern() 
      && klass.prototype_header().has_bias_pattern()) {

        // 构造一个偏向请求线程的 markWord
        MarkWord new_mark = create_mark(requestiong_thread, lock.get_mark().get_age(), klass.get_prototype_header().bias_epoch())
        // 更新当前锁对象的mark word
        lock.set_mark_word(new_mark);
        status_code = BiasedLocking::BIAS_REVOKED_AND_REBIASED;
    }
    return status_code;
}
```

批量撤销和批量重偏向的执行过程相似的, 不同的是一个是更新 class 的 epoch, 一个是将 class 的偏向锁模式修改为无锁。
同样的下面的对每个线程里面的锁记录进行撤销的操作, 在测试的时候没有发现这个效果

## 2.5 锁释放

释放: 在偏向锁的操作中就是退出同步块, 很简单的操作

[源代码地址](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/9ce27f0a4683/src/share/vm/interpreter/bytecodeInterpreter.cpp#l1923)

**伪代码**

```java
case (_monitorexit): {

    //  获取当前锁对象
    Lock lock = get_lock();

    // 从栈中获取一个 锁记录
    LockRecord limit = get_monitor_base();
    LockRecord most_recent = get_stack_base();

    // 从低往高遍历栈/栈帧的Lock Record
    while(most_recent != limit) {
        // Lock Record 里面的锁对象等于当前锁对象
        if (most_recent.get_obj() == lock) {

            Lock temp_lock = most_recent.get_lock();
            MarkWord mark = most_recent.get_displace_header();
            
            // 偏向锁只需要释放 Lock Record
            most_recent.set_obj(null);

            // 不是偏向锁, 下面的逻辑是轻量级锁+重量级锁的逻辑了
            if (!lock.get_mark_word().has_bias_pattern()) {
                // 省略
            }

            //执行下一条命令
            UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);
        }
        //处理下一条Lock Record
        most_recent++;
    }

    // Need to throw illegal monitor state exception
    // 非法情况的处理
    CALL_VM(InterpreterRuntime::throw_illegal_monitor_state_exception(THREAD), handle_exception);
    ShouldNotReachHere();
}
```

## 2.6 参考
[偏向锁的【批量重偏向与批量撤销】机制](https://www.it610.com/article/1296551396493041664.htm)  
[死磕Synchronized底层实现--偏向锁](https://github.com/farmerjohngit/myblog/issues/13)  
《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版》 -- 周志明