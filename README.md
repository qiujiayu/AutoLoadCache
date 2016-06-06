# AutoLoadCache
---------------------------------------------
[![Build Status](http://img.shields.io/travis/qiujiayu/AutoLoadCache.svg?style=flat&branch=master)](https://travis-ci.org/qiujiayu/AutoLoadCache)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.qiujiayu/autoload-cache.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.qiujiayu/autoload-cache/)
![GitHub license](https://img.shields.io/github/license/qiujiayu/AutoLoadCache.svg?style=flat-square)


现在使用的缓存技术很多，比如*Redis*、 *Memcache* 、 *EhCache*等，甚至还有使用*ConcurrentHashMap* 或 *HashTable* 来实现缓存。但在缓存的使用上，每个人都有自己的实现方式，大部分是直接与业务代码绑定，随着业务的变化，要更换缓存方案时，非常麻烦。接下来我们就使用**AOP + Annotation** 来解决这个问题，同时使用**自动加载机制** 来实现数据“**常驻内存**”。

Spring AOP这几年非常热门，使用也越来越多，但个人建议AOP只用于处理一些辅助的功能（比如：接下来我们要说的缓存），而不能把业务逻辑使用AOP中实现，尤其是在需要“事务”的环境中。


###[设计思想及原理](idea.md)

###[使用方法](use.md)

###[注解（Annotation）说明](annotations.md)

###[表达式的应用](script.md)

###[缓存删除](deleteCache.md)

###[注意事项](warning.md)

###[缓存管理页面](admin.md)

###[与Spring Cache的区别](SpringCache.md)

### [更新日志](changesLog.md)

###未来计划：redis3.0 集群研究以及使用

QQ群：429274886