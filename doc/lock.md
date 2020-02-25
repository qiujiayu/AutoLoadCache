## 分布式锁配置

为了支持在高并发，大集群环境使用，增加了分布式锁的。

现在只支持排它锁，还不支持重入锁。

需要开启分布式锁支持，需要做以下配置：

1. 在[@Cache](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/Cache.java "@Cache")注解中设置 lockExpire, 并且opType必须是CacheOpType.READ_WRITE。
2. 需要往 [CacheHandler](../autoload-cache-core/src/main/java/com/jarvis/cache/CacheHandler.java "CacheHandler") 中的lock 属性注入ILock实例。 框架中已经内部已经基于Redis实现了一些分布式锁：

  * ILock 
<<<<<<< HEAD
      * |-----[AbstractRedisLock](../src/main/java/com/jarvis/cache/lock/AbstractRedisLock.java "AbstractRedisLock")
          * |-----[JedisClusterLock](../src/main/java/com/jarvis/cache/lock/JedisClusterLock.java "JedisClusterLock")
          * |-----[ShardedJedisLock](../src/main/java/com/jarvis/cache/lock/ShardedJedisLock.java "ShardedJedisLock")
=======
      * |-----[AbstractRedisLock](../autoload-cache-lock/autoload-cache-lock-redis/src/main/java/com/jarvis/cache/lock/AbstractRedisLock.java "AbstractRedisLock")
          * |-----[JedisClusterLock](../autoload-cache-lock/autoload-cache-lock-jedis/src/main/java/com/jarvis/cache/lock/JedisClusterLock.java "JedisClusterLock")
          * |-----[ShardedJedisLock](../autoload-cache-lock/autoload-cache-lock-jedis/src/main/java/com/jarvis/cache/lock/ShardedJedisLock.java "ShardedJedisLock")
>>>>>>> modules
