# 1 CollectionAndMapBrief

Java 的集合框架是我们平时使用频率挺高的代码, 基本涉及到数据的临时存储, 都能见到他们的身影。 但是在庞大的集合类中, 找到一个最适合自己需求的实现类，则需要对 Java 的集合框架有一定的了解。 本文将从全局出发，整理一下 Java 的集合框架。

## 1.1 分类

整个 Java 集合框架的关系图如下(本图为基础版):  


![Alt 'JavaCollectionAndMapUML'](https://raw.githubusercontent.com/PictureRespository/Java/main/CollectionAndMap/JavaCollectionAndMapUML.png)

从图中我们可以看出, Java 的集合框架大体分成 2 派: Collection 和 Map。

Collection主要有3个成员 
> 1. List 存储在内部的数据是有序的, 同时可重复
> 2. Set 不能有重复元素
> 3. Queue 队列, 可以保证数据的先进先出

Map 则比较简单, 是一个 key-value 模式的存储结构，要求 key 不能重复，通过 key 可以获取到唯一的 value。

## 1.2 具体介绍

### 1.2.1 List

特点:
> 1. 有序 (元素存入集合的顺序和取出的顺序一致), 元素都有索引, 同时支持重复元素。  
> 2. 除了具有 Collection 接口必备的 iterator() 方法外, List 还提供一个 listIterator() 方法, 返回一个 ListIterator 接口。Iterator 的遍历都是从头开始一直往后遍历的，但是 listIterator 可以指定从哪个位置开始遍历, 同时支持往前遍历，同时增加元素, 修改元素。


优点：操作读取操作效率高，基于数组实现的，可以为null值，可以允许重复元素，有序，异步。  
缺点：由于它是由动态数组实现的，不适合频繁的对元素的插入和删除操作，因为每次插入和删除都需要移动数组中的元素。


> 1. ArrayList 
> 2. LinkedList
> 3. Vector 可以看成是一个线程安全的ArrayList, 因为他的内部大部分的方法都是通过 `synchronized` 进行修饰的, 和 ArrayList 的区别是, 支持设置每次扩容的容量 (ArrayList 默认是当前的 0.5 倍), 如果没有设置, Vector 默认是当前的 1 倍, Vector 不被推荐使用了
> 4. Stack 继承了 Vector, 同时提供了符合栈特点的 5 个方法： push, pop, peek, search, empty。 研究 Stack 本质就是在研究 Vetor, 所以这里就不进行讲解了。 而且 Stack 类官方已经不在建议使用了 (Stack 的实现有许多不规范的地方), 现在官方推荐使用 `java.util.Deque`。

### 1.2.2 Set 

> 1. 无序(存入和取出顺序有可能不一致), 不可以存储重复元素
> 2. 当存入的元素重复了, 后面的会被替换

> 1. HashSet
> 2. TreeSet [详情](https://www.jianshu.com/p/12f4dbdbc652)


### 1.2.3 Queue

> 1. Queue 用于模拟队列这种数据结构，实现 'FIFO' 等数据结构
> 2. 队列常作被当作一个可靠的将对象从程序的某个区域传输到另一个区域的途径
> 3. 队列不允许随机访问队列中的元素  
> 4. 使用 Queue 实现通常不允许插入 null 元素, 因为 null 也用作 poll 方法的一个特殊返回值，表明队列不包含元素了。
> 5. 在 Java 中 和 Queue 直接关联的类有 BlockingQueue (还是一个接口, 只是继承了 Queue 的方法), Deque (还是一个接口, 为了规范为双向队列, 还添加了一下自身的方法), AbstractQueue (抽象类, 封装了一层)

> 1. ArrayDeque
> 1. ArrayBlockingQueue [详情](http://benjaminwhx.com/2018/05/07/%E3%80%90%E7%BB%86%E8%B0%88Java%E5%B9%B6%E5%8F%91%E3%80%91%E8%B0%88%E8%B0%88ArrayBlockingQueue/)
> 2. LinkedBlockingQueue [详情](http://benjaminwhx.com/2018/05/11/%E3%80%90%E7%BB%86%E8%B0%88Java%E5%B9%B6%E5%8F%91%E3%80%91%E8%B0%88%E8%B0%88LinkedBlockingQueue/)


### 1.2.4 Map

> 1. Map 主要用于存储健值对, 根据键得到值, 因此不允许键重复, 但允许值重复
> 2. 是一个双列集合，有两个泛型 key 和 value，使用的时候 key 和 value 的数据类型可以相同, 也可以不同

>1. HashMap
>2. TreeMap [详情](https://cloud.tencent.com/developer/article/1121260)
>3. Hashtable [详情](https://www.cnblogs.com/skywang12345/p/3310887.html)
>4. LinkedHashMap [详情](https://www.jianshu.com/p/8f4f58b4b8ab)
>5. Properties [详情](https://www.jianshu.com/p/52f8ad17d54a)


## 1.3 使用情景

### 1.3.1 List

数据可重复, 同时有序

**ArrayList**  
> 1. 基于数组实现的, 同时支持动态扩容  
> 2. 随机访问效率高，随机插入、随机删除效率低  
> 3. 线程不安全
> 4. 允许放入 null, 同时支持多个 null

**LinkedList**  
> 1. 基于双向链表实现
> 2. 随机访问效率低，但随机插入、随机删除效率高
> 3. 线程不安全
> 4. 允许放入 null, 同时支持多个 null

**Vector**  
> 1. 基于数组实现的, 同时支持动态扩容
> 2. 随机访问效率高，随机插入、随机删除效率低  
> 3. 线程安全
> 4. 为了线程安全, 大部分的方法都通过 synchronized 进行修饰, 所以在效率上比 ArrayList 慢一些
> 5. 允许放入 null, 同时支持多个 null
> 6. 不推荐使用 Vector, 大部分都加锁，导致了效率低, 而这个加锁很多时候都是你不需要的 

**Stack**
> 1. 继承于 Vector, 具备了 Vector 的所有特性
> 2. 同样的不推荐使用 Stack, 如果需要使用栈的话，官方推荐的是 ArrayDeque


### 1.3.2 Set

数据唯一, 数据无序

**HashSet**  
> 1. 基于 HashMap 实现的, 也就是 数组 + 链表 + 红黑树 实现的。
> 2. 我们放入的内容, 存在了内部 HashMap 的key, value 统一为 Object
> 3. 线程不安全
> 4. 允许存入 null, 但是只支持 1 个, 默认后者覆盖前者


**TreeSet**
> 1. 基于 TreeMap 实现的, 也就是红黑树
> 2. TreeMap 存入的数据是有序的, 默认按照存入的值的 hashCode 的大小进行排序。 也可以通过实现 Comparator 接口, 自定义排序规则。
> 3. 线程不安全
> 4. 不允许存 null

### 1.3.3 Queue

数据可重复, 同时有序, 但是存入和取出的顺序是相反的

**ArrayDeque**
> 1. 基于数组实现的双端队列
> 2. 随机访问效率高，随机插入、随机删除效率低
> 3. 线程不安全
> 4. 不允许存入 null
> 5. 不仅可以当做双端队列使用，还可以用于栈

**ArrayBlockingQueue**
> 1. 基于数组实现的阻塞队列, 不支持扩容
> 2. 线程安全的
> 3. 不允许存入 null

**LinkedBlockingQueue**
> 1. 基于双向链表实现的
> 2.随机访问效率低，但随机插入、随机删除效率高
> 3. 线程安全
> 4. 不允许存入 null

### 1.3.4 Map

数据需要有 key-value 的映射关系

**HashMap**
> 1. 基于 数组 + 链表 + 红黑树 实现的
> 2. 存入的数据是无序的,
> 3. 线程不安全
> 4. 允许存入 null, 但是只支持 1 个, 默认后者覆盖前者

**TreeMap**
> 1. 基于 红黑树 实现的
> 2. 存入的数据是有序的, 默认按照存入的值的 hashCode 的大小进行排序。 也可以通过实现 Comparator 接口, 自定义排序规则。
> 3. 线程不安全
> 4. key 不允许存入 null, value 可以

**Hashtable**
> 1. 基于数组 + 链表实现的
> 2. 线程安全的
> 3. key 不能为 null, value 也不能为 null

**LinkedHashMap**
> 1. 基于数组 + 双向链表 实现的
> 2. 线程非安全的
> 3. key, value 都能为 null

**Properties**
> 1. 基于 Hashtable 实现的，具有 Hashtable 的特点

## 1.4 参考  
[集合类--最详细的面试宝典--看这篇就够用了(java 1.8)](https://www.cnblogs.com/yysbolg/p/9230184.html)  
[从 java.util.Stack 的原理说它为什么不被官方所推荐使用！](https://www.xttblog.com/?p=3416)

