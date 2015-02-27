# AutoLoadCache
现在使用的缓存技术很多，比如Redis、 Memcache 、 EhCache等，甚至还有使用ConcurrentHashMap或HashTable来实现缓存。但在缓存的使用上，每个人都有自己的实现方式，大部分是直接与业务代码绑定，随着业务的变化，要更换缓存方案时，非常麻烦。接下来我们就使用AOP + Annotation 来解决这个问题，同时使用自动加载机制来实现数据“常驻内存”。

