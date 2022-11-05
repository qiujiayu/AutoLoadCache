# AutoLoadCache
---------------------------------------------
[![Build Status](http://img.shields.io/travis/qiujiayu/AutoLoadCache.svg?style=flat&branch=master)](https://travis-ci.org/qiujiayu/AutoLoadCache)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.qiujiayu/autoload-cache-parent.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.qiujiayu/autoload-cache-parent/)
![GitHub license](https://img.shields.io/github/license/qiujiayu/AutoLoadCache.svg?style=flat-square)


现在使用的缓存技术很多，比如*Redis*、 *Memcache* 、 *EhCache*等，甚至还有使用*ConcurrentHashMap* 或 *HashTable* 来实现缓存。但在缓存的使用上，每个人都有自己的实现方式，大部分是直接与业务代码绑定，随着业务的变化，要更换缓存方案时，非常麻烦。接下来我们就使用**AOP + Annotation** 来解决这个问题，同时使用**自动加载机制** 来实现数据“**常驻内存**”。


### [设计思想及原理](./doc/idea.md)

在infoq 发表的文章[《面对缓存，有哪些问题需要思考？》](http://www.infoq.com/cn/articles/thinking-about-distributed-cache-redis)

[《再谈缓存的穿透、数据一致性和最终一致性问题》](https://mp.weixin.qq.com/s?__biz=MzIwMzg1ODcwMw==&mid=2247487343&idx=1&sn=6a5f60341a820465387b0ffcf48ae85b)

### [使用方法](./doc/use.md)

### [注解（Annotation）说明](./doc/annotations.md)

### [分布式锁支持](./doc/lock.md)

### [表达式的应用](./doc/script.md)

### [缓存删除](./doc/deleteCache.md)

### [Magic模式](./doc/magic.md)

### [注意事项](./doc/warning.md)

### [与Spring Cache的区别](./doc/SpringCache.md)

### [最佳实战](./doc/suggest.md)

### [autoload-cache-spring-boot-starter](./autoload-cache-spring-boot-starter) 推荐使用这个，[test目录](./autoload-cache-spring-boot-starter/tree/master/src/test)中也有可运行例子。


### 源码阅读

已经实现基于aspectj 的AOP，代码在[com.jarvis.cache.aop.aspectj.AspectjAopInterceptor](./autoload-cache-aop/autoload-cache-aop-aspectj/src/main/java/com/jarvis/cache/aop/aspectj/AspectjAopInterceptor.java)。想通过阅读代码了解详细细节，可以以此为入口。

### [更新日志](./doc/changesLog.md)



QQ群：429274886
