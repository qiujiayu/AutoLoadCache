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

### [缓存管理页面](./doc/admin.md)

### [与Spring Cache的区别](./doc/SpringCache.md)

### [最佳实战](./doc/suggest.md)

### [Spring boot demo](https://github.com/qiujiayu/AutoLoadCache-spring-boot)

### 源码阅读

已经实现基于aspectj，代码在com.jarvis.cache.aop.aspectj.AspectjAopInterceptor。想通过阅读代码了解详细细节，可以以此为入口。

注意：有些类get, set, hashCode()， toString() equals()等方法是使用 lombok 自动生成的，所以使用Eclipse 和 IntelliJ IDEA 时，需要先安装lombok。


### [更新日志](./doc/changesLog.md)

### 未来计划：
希望未来能更好适应高并发的环境，更方便运维，欢迎有这方面经验的人能参与进来，让更多的人受益。

在异步刷新缓存时，增加尝试多次去数据层加载数据，以适应解决有多个数据源，而其中部分数据源出问题情况。通过这种尝试机制，也许能获取到新的数据。

增加缓存过期时间管理策略，暂时想到三种：

    1. 直接使用@Cache中设置的缓存时间，在缓存即将过期时，刷新缓存；
    2. 根据@Cache中设置的缓存时间乘以n，如果缓存已经到了@设置的过期时间，则进行异步刷新。比如n=2，@Cache中设置10分钟，则实际缓存时间为20分钟，当过了10分钟后就可以刷新缓存；
    3. 根据@Cache中设置的缓存时间加上n分钟。原理和第2种差不多。第二和第三种我们可以认为是“主动续租”。
    
增加连接缓存服务器异常处理机制，比如，当每秒连接错误达到一定数量后，增加重试机制，降低去数据层获取数据的速率，启用本地临时缓存，随机放弃一部分请求等等。

如果在DataLoader中loadData时，等待第一个请求返回数据超时时，再去缓存中获取一次，如果还是没有，则随机个线程去数据层获取数据或控制获取数据的速率，或放弃一些请求。

如果系统A依赖系统B的数据，如何能实现在系统A，就能直接获取系统B中缓存数据，这样不仅能减少访问接口的并发，而且能更好保证缓存中数据的一致性。


guava cache的研究和使用。参考Spring 中org.springframework.cache.guava.GuavaCacheManager相关代码。


QQ群：429274886