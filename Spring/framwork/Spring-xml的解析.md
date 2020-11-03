# Spring xml 的解析


Spring 内部是使用 SAX(simple api for xml) 进行解析的。

相对于 DOM(一次性把xml文件加载到内存里，如果是大文件的话，很占内存，影响性能) 的解析方法, SAX 是事件驱动的流式解析方式，并不是把 xml 全部加载到内存，而是一边读取一边解析，不可暂停或者倒退，直到结束。

DOM 在解析中可以对元素进行 crud, 而 SAX 不可以。
