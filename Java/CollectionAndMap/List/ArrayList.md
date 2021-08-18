# 1 ArrayList

ArrayList 一个动态数组。本身是基于 Array 进行封装的列表, 提供了自动扩容的功能: 在容量不够的情况下, 自动扩容。  
支持存储包括 null 在内的所有数据类型。

## 1.1 Array （数组）

了解 ArrayList 之前, 我们需要先了解一下数组的特点

>1. 数组的内存是连续的，不存在相邻元素之间还隔着其他内存
>2. 数组内存储的数据类型都是一样的
>3. 数组具备了查询快，增删慢的特点
>4. 数组在声明的时候需要知道初始的容量, 一个容量一旦确定了, 就不能再改变了

基于 Array  实现的 ArrayList, 同理也具备了相同的特性。

## 1.2 ArrayList 中的几个属性

```java
transient Object[] elementData; 
```

ArrayList 是基于 Array 实现的，对 Array 做的封装。所以其本身还是通过 Array 进行数据的存储的。
而 elementData 就是真正存储数据的地方。


```java
private static final int DEFAULT_CAPACITY = 10;
```

默认的初始容量大小， 数组在使用的时候需要提前声明容量。在声明  ArrayList 时, 如果没有指定需要的容量大小, 默认就是 10


```java
private static final Object[] EMPTY_ELEMENTDATA = {};
```

在声明 ArrayList 时, 支持不显示声明容量大小, 则默认为 10。 也可以手动声明大小, 声明了多少, 那么容量就是多少。
所以用户可以声明容量为 0, 当声明的容量为 0。 那么声明的数组就等于这个 `elementData = EMPTY_ELEMENTDATA;`。  

这个属性是通过 static 修饰的, 内存中只会存在一份。那么当所有声明容量为 0 的 ArrayList, 内部的数组都是这个，节省内存。


```java
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
```

在声明 ArrayList 时, 不指定容量的声明, 会先将 ArrayList 存储数据的 elementData 先赋值为 0 的这个属性 `elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA`。  
在进行数据存储时, 再进行扩容。

同样的声明为 static, 可以共用。  同时不在声明时就立即进行包含容量的数组声明。可以防止声明了 ArrayList 但是没用到的情况, 浪费内存。

```java
private int size;
```

真正存储的数据个数。


## 1.3 ArrayList 的构造方法

### 1.3.1 不包含任何参数的构造函数

```java
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
```

很简单, 将空数组的 DEFAULTCAPACITY_EMPTY_ELEMENTDATA 赋值给存储数据的 elementData。  

延迟声明有容量的数组, 防止声明了, 但是没有使用的 ArrayList, 浪费空间。  
同时先赋值为静态的 DEFAULTCAPACITY_EMPTY_ELEMENTDATA, 多个 ArrayList 可以共用这个, 节省内存。

### 1.3.2 指定容量的构造函数

```java
public ArrayList(int initialCapacity) {

    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException("Illegal Capacity: "+ initialCapacity);
    }
}
```

> 1. 指定的容量小于 0, 抛异常
> 2. 指定的容量为 0, 将空数组 EMPTY_ELEMENTDATA 赋值给 elementData
> 3. 指定的容量大于 0, 将 elementData 声明指定容量的数组

### 1.3.3 给定一个 Collection 的构造函数

```java
public ArrayList(Collection<? extends E> c) {

    Object[] a = c.toArray();

    if ((size = a.length) != 0) {

        if (c.getClass() == ArrayList.class) {
            elementData = a;
        } else {
            elementData = Arrays.copyOf(a, size, Object[].class);
        }

    } else {
        elementData = EMPTY_ELEMENTDATA;
    }
}

```

> 1. 传入的 Collection 的长度为 0, 将空数组 EMPTY_ELEMENTDATA 赋值给 elementData
> 2. 传入的 Collection 的长度不等于 0, 将传入的 Collection 转为数组
> 3. 传入的 Collection 为 ArrayList 类型， 将转换的数组赋值给 elmentData
> 4. 传入的 Collection 不是 ArrayList 类型, 则需要将转换后的数组转换为 Object 数组, 在赋值给 elementData

上面 3, 4 步存在的原因是, Collection.toArray 方法有一些实现的差异。具体的分析可以看最后面的分析、

## 1.4 ArrayList 操作方法

### 1.4.1 添加数据

```java
public boolean add(E e) {
    
    // 确保容量足够
    // 容量够, 不做处理
    // 容量不够, 动态的扩容
    ensureCapacityInternal(size + 1);

    // 放入到数组的最后一位
    elementData[size++] = e;
    return true;
}

/**
 * 确保容量如果, 容量不够, 进行扩容
 */
private void ensureCapacityInternal(int minCapacity) {
    
    // calculateCapacity 需要的容量
    ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}

/**
 * 计算一下当前需要多少容量
 */
private static int calculateCapacity(Object[] elementData, int minCapacity) {

    // DEFAULTCAPACITY_EMPTY_ELEMENTDATA: 前面声明的空数组 {}
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        // DEFAULT_CAPACITY: 默认值 10
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}

/**
 * 确保当然数组的长度比需要的最小的容量大, 不够的话，自动扩容
 */
private void ensureExplicitCapacity(int minCapacity) {

    modCount++;

    // 需要的最少容量比当前的数组的容量大, 说明当前的数组容量不够了, 需要进行扩容
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

/**
 * 数组容量扩容， 默认是当前容量的 1.5 倍
 * 如果当前容量的 1.5 倍比需要的最小容量还小，直接取最小容量
 * 
 */
private void grow(int minCapacity) {

    int oldCapacity = elementData.length;

    // oldCapacity >> 1  相当于  oldCapacity/2 的一次方  也就是  oldCapacity * 0.5
    // 也就是 数组的扩容 = 原来的容量 * 1.5
    int newCapacity = oldCapacity + (oldCapacity >> 1);

    // 计算出来的容量比需要的最小的容量还小, 则直接去最小的容量
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;

    // MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8
    // 计算出来的新的容量比 Integer 的最大值 - 8 还大
    // 主要是为了防止 计算出来的容量超过了 Integer 的最大值
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);

    // 创建一个新的数组，同时把当前的数组内容转移到新的数组
    elementData = Arrays.copyOf(elementData, newCapacity);   
}

/**
 * 确保当前的容量不超过 Integer 的最大值
 */
private static int hugeCapacity(int minCapacity) {
    
    // 小于 0, 超过了, 直接抛异常
    if (minCapacity < 0)
        throw new OutOfMemoryError();
    // 容量 >  Integer.MAX_VALUE - 8 的话, 直接返回 Integer 最大值, 否则返回 Integer.MAX_VALUE - 8   
    return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
}
```

### 1.4.2 其他方式的添加数据

**指定位置的插入**

```java
public void add(int index, E element) {

    // 判断一下 index 是否正常, index 不能大于 size, index 不能小于 0
    rangeCheckForAdd(index);
    // 走一遍判断扩容
    ensureCapacityInternal(size + 1); 

    // 新建一个数组  数组的内容和当前的数据一样，把index后面的数据都往后退一位, 然后 index 位置变为 null
    System.arraycopy(elementData, index, elementData, index + 1, size - index);

    // 设置 index 的元素为要插入的数据
    elementData[index] = element;
    size++;
}


private void rangeCheckForAdd(int index) {
    if (index > size || index < 0)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}
```

指定位置的数据插入会导致数组数据的转移, 影响性能。

**直接插入一个 Collection**

```java
public boolean addAll(Collection<? extends E> c) {

    Object[] a = c.toArray();
    int numNew = a.length;
    
    // 确保容量足够
    ensureCapacityInternal(size + numNew); 
    
    // 新建一个新的数组, 拷贝原来数组和需要插入的 Collection 转换为的数组到新数组
    System.arraycopy(a, 0, elementData, size, numNew);
    
    size += numNew;
    return numNew != 0;
}
```

**在指定的位置插入一个 Collection**

```java

public boolean addAll(int index, Collection<? extends E> c) {

    // 判断一下 index 是否正常, index 不能大于 size, index 不能小于 0
    rangeCheckForAdd(index);

    Object[] a = c.toArray();
    int numNew = a.length;

    // 确保容量足够
    ensureCapacityInternal(size + numNew);
    // 计算需要移动的位置
    int numMoved = size - index;

    if (numMoved > 0)
        // 新建一个数组, 将 elementData 从 index 后面的数据移动到 elementData 的 index + numNew 的后面
        System.arraycopy(elementData, index, elementData, index + numNew, numMoved);

    // 将 a 移动到新数组的 index 和 index + numNex 之间
    System.arraycopy(a, 0, elementData, index, numNew);
    size += numNew;
    return numNew != 0;
}
```

### 1.4.4 删除数据

**删除指定位置的元素**

```java
public E remove(int index) {

    // 判断 index 是否合法
    rangeCheck(index);

    modCount++;
    
    // 取到需要删除的元素
    E oldValue = elementData(index);

    // 删除的元素所在的位置后面还有多少个元素
    int numMoved = size - index - 1;

    if (numMoved > 0)
        // 将 elementData index + 1 开始到 numMoved 个元素依次放到 elementData 的 index 处的后面
        System.arraycopy(elementData, index+1, elementData, index, numMoved);
    // 设置 site - 1 处的元素为空
    elementData[--size] = null;
}

/**
 * index 检查
 */
private void rangeCheck(int index) {
    // index 不能大于 size
    if (index >= size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

/**
 * 获取需要删除的元素
 */
E elementData(int index) {
    return (E) elementData[index];
}
```

**删除指定的元素**

```java
public boolean remove(Object o) {
 
    // o 为 null, 则将 0 到 size 之间第一个 null 删除
    if (o == null) {
        for (int index = 0; index < size; index++) {
            if (elementData[index] == null) {
                fastRemove(index);
                return true;
            }
        }
    } else {

        for (int index = 0; index < size; index++) {
            // 调用 Object 的 equals 比较 2 个对象是否为同一个
            if (o.equals(elementData[index])) {
                fastRemove(index);
                return true;
            }
        }
    }
    return false;
}


/**
 * 删除元素和 remove(int) 类型
 */
private void fastRemove(int index) {
    modCount++;
    int numMoved = size - index - 1;
    // 将 elementData index + 1 开始到 numMoved 个元素依次放到 elementData 的 index 处的后面
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index, numMoved);

    // 设置 site - 1 处的元素为空
    elementData[--size] = null; 
}

```

## 1.5 ArrayList 补充





## 1.6 Collection.toArray() 

可以先查看[这里](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6260652), 了解一下, Collection.toArray() 的问题。

下面整理一下:


### 1.6.1 对象向上转型

```java

private static void test1() {

    String str = "123";
	Object obj = str;
	Object obj2 = obj;

	// class java.lang.String
	System.out.println(str.getClass());

	// class java.lang.String
	System.out.println(obj.getClass());

	// class java.lang.String
	System.out.println(obj2.getClass());


	Integer num = 1;
	Number obj3 = num;

	// class java.lang.Integer
	System.out.println(num.getClass());

	// class java.lang.Integer
	System.out.println(obj3.getClass());
}
```

从上面的代码, 可以发现通过向上转型, **虽然对象已经是转型后的类型了，但是还是会保留了实际的类型**。

### 1.6.2 对象为数组时, 向上转型

```java

private static void test2() {

    Object[] obj = new Object[2];
	obj[0] = new String();
    Object[] strObj = new String[2];

    // class [Ljava.lang.Object;
    System.out.println(obj.getClass());

    // class [Ljava.lang.String;
    System.out.println(strObj.getClass());

    // class java.lang.String
    System.out.println(obj[0].getClass());

}
```

从上面的代码, 可以发现**声明的数组是什么类型，那么他的类型就是什么**。 但是**放入到内部的对象, 通过转型存到了数组里面, 但是它的实际类型还是没变的**。

### 1.6.3 Collection.toArray()

```java
private static void test3() {

    List<String> list = new ArrayList<>();
	list.add("123");
	Object[] objArray = list.toArray();

    List<String> list2 = Arrays.asList("123");
	Object[] objArray2 = list2.toArray();

    // class [Ljava.lang.Object;
	System.out.println(objArray.getClass());

	// class [Ljava.lang.String;
	System.out.println(objArray2.getClass());
}
```

在上面中通过 Collection.toArray() 方法得到的 2 个 Object[], 一个是类型的 Object 数组, 一个是类型是原来的类型的数组。  
同样的方法但是返回的 2 种不同的结果。 所以才有通过一个 Collection 创建 ArrayList, 才会判断一下 `toArray` 方法返回的数据的实际类型。

至于 `Object[] toArray();` 返回的 Object[] 是不同的类型, 需要具体到不同的实现

```java
public class ArrayList<E> {

    // 调用到 Array.copyOf 方法
    public Object[] toArray() {

        // elementData.getClass 是 Object[] 类型, 所以通过 Array.copyOf 得到的为 Object[] 类型
        return Arrays.copyOf(elementData, size);
    }
}

public class Arrays {

    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }
}
```

```java

public class Arrays {

    public static <T> List<T> asList(T... a) {
        // 通过 Arrays.asList 方法返回的是内部自行实现的 ArrayList
        return new ArrayList<>(a);
    }

    private static class ArrayList<E> {

        // 常用的 ArrayList 的数据是使用一个 Object[] elementData 进行保存的， 这里使用的是泛型数组存储
        // 区别是 elementData.getClass 返回的是 Object[], 这里 a.getClass 返回的是实际数据的类型
        private final E[] a;

        ArrayList(E[] array) {
            a = Objects.requireNonNull(array);
        }

        // 自行实现的 ArrayList, 内部不是调用自身的 copyOf 进行转换的，而是通过 clone 方法
        // 通过 clone() 返回结果的是 E[], 然后通过向上转型 (Object[])E[], 所以返回了 Object[], 但是保留了原本的类型。
        public Object[] toArray() {
            return a.clone();
        }
    }
}
```