# AutoLoadCache 与 Spring Cache 的区分
---------------------------------------------
AutoLoadCache 与 Spring Cache 相似，都是使用 AOP + Annotation ，将缓存与业务逻辑进行解耦。比较重要的分别就是：AutoLoadCache 实现了自动加载和“拿来主义”机制，能更好地解决系统的性能及并发问题。

Spring Cache使用name 和 key的方式来管理缓存，而AutoLoadCache 使用的是key + hfield + expire 来管理缓存。也说明Spring Cache 比较适合用来管理Ehcache的缓存，而AutoLoadCache 适合管理Redis, Memcache 以及 ConcurrentHashMap，尤其是Redis和 ConcurrentHashMap，hfield 相关的功能都是针对它们进行开发的（因为Memcache不支持hash表，所以没办法使用hfield相关的功能）。

Spring Cache 最大的缺点就是无法使用Spring EL表达式来动态生成Cache name,而且Cache name是的必须在Spring 配置时指定几个，非常不方法使用。尤其想在Redis中想精确清除一批缓存，是无法实现的，可能会误删除我们不希望被删除的缓存。

AutoLoadCache中使用了一致性hash算法对Redis缓存进行分片处理。如果在Spring Cache 中想使用一致性hash算法进行分片那是不太可能的，即使实现了，也会给清缓存带来麻烦。