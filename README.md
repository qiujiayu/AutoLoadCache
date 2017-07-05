# AutoLoadCache
---------------------------------------------
[![Build Status](http://img.shields.io/travis/qiujiayu/AutoLoadCache.svg?style=flat&branch=master)](https://travis-ci.org/qiujiayu/AutoLoadCache)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.qiujiayu/autoload-cache.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.qiujiayu/autoload-cache/)
![GitHub license](https://img.shields.io/github/license/qiujiayu/AutoLoadCache.svg?style=flat-square)


现在使用的缓存技术很多，比如*Redis*、 *Memcache* 、 *EhCache*等，甚至还有使用*ConcurrentHashMap* 或 *HashTable* 来实现缓存。但在缓存的使用上，每个人都有自己的实现方式，大部分是直接与业务代码绑定，随着业务的变化，要更换缓存方案时，非常麻烦。接下来我们就使用**AOP + Annotation** 来解决这个问题，同时使用**自动加载机制** 来实现数据“**常驻内存**”。


### [设计思想及原理](./doc/idea.md)

### [使用方法](./doc/use.md)

### [注解（Annotation）说明](./doc/annotations.md)

### [分布式锁支持](./doc/lock.md)

### [表达式的应用](./doc/script.md)

### [缓存删除](./doc/deleteCache.md)

### [注意事项](./doc/warning.md)

### [与Spring Cache的区别](./doc/SpringCache.md)

### [最佳实战](./doc/suggest.md)

### [Spring boot demo](https://github.com/qiujiayu/AutoLoadCache-spring-boot)

### [autoload-cache-spring-boot-starter](https://github.com/qiujiayu/autoload-cache-spring-boot-starter)

### 源码阅读

已经实现基于aspectj 的AOP，代码在[com.jarvis.cache.aop.aspectj.AspectjAopInterceptor](./src/main/java/com/jarvis/cache/aop/aspectj/AspectjAopInterceptor.java "AspectjAopInterceptor.java")。想通过阅读代码了解详细细节，可以以此为入口。

注意：有些类get, set, hashCode()， toString() equals()等方法是使用 lombok 自动生成的，所以使用Eclipse 和 IntelliJ IDEA 时，需要先安装lombok。


### [更新日志](./doc/changesLog.md)



QQ群：429274886