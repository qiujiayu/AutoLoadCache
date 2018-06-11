## 使用方法


### 1. Maven dependency:

    <dependency>
      <groupId>com.github.qiujiayu</groupId>
      <artifactId>autoload-cache</artifactId>
      <version>${version}</version>
    </dependency>

### 2. [AutoLoadConfig 配置说明](AutoLoadConfig.md)

### 3. 序列化工具：

序列化工具主要用于深度复杂，以及缓存中数据与Java对象的转换。框架中已经实现如下几种序列化工具:

1.  com.jarvis.cache.serializer.HessianSerializer 基于Hessian2序列化工具
2.  com.jarvis.cache.serializer.JdkSerializer JDK自带序列化工具
3.  com.jarvis.cache.serializer.FastjsonSerializer 基于Fastjson序列化工具，使用Fastjson时需要注意：返回值中如果是泛型的话，需要指明具体的类型，比如：List<User>，如果是直接返回List则会出错。

如果希望对比较长的数据进行压缩处理后再传到分布式缓存服务器的话，可以使用com.jarvis.cache.serializer.CompressorSerializer 进行处理。支持GZIP，BZIP2，XZ，PACK200，DEFLATE，等几种压缩算法（默认使用GZIP）。

如果需要使用其它序列化工具，可以通过实现com.jarvis.cache.serializer.ISerializer<Object>来扩展（比如：Kryo和FST等）。

    <bean id="hessianSerializer" class="com.jarvis.cache.serializer.HessianSerializer" />
    <bean id="jdkSerializer" class="com.jarvis.cache.serializer.JdkSerializer" />
    <bean id="fastjsonSerializer" class="com.jarvis.cache.serializer.FastjsonSerializer" />

    <bean id="hessianCompressorSerializer" class="com.jarvis.cache.serializer.CompressorSerializer">
      <constructor-arg ref="hessianSerializer" />
    </bean>

### 4. 表达式解析器

缓存Key及一些条件表达式，都是通过表达式与Java对象进行交互的，框架中已经内置了使用Spring El和Javascript两种表达的解析器，分别的：com.jarvis.cache.script.SpringELParser 和 com.jarvis.cache.script.JavaScriptParser，如果需要扩展，需要继承com.jarvis.cache.script.AbstractScriptParser 这个抽象类。

    <bean id="scriptParser" class="com.jarvis.cache.script.SpringELParser" />

### 5.缓存配置

框架已经支持 Redis、Memcache以及ConcurrentHashMap 三种缓存：

* [Redis 配置](JRedis.md)
* [Memcache 配置](Memcache.md)
* [ConcurrentHashMap 配置](ConcurrentHashMap.md)
* [二级缓存请参考ComboCacheManager.java](../src/main/java/com/jarvis/cache/ComboCacheManager.java)

### 6.缓存处理器

    <bean id="cacheHandler" class="com.jarvis.cache.CacheHandler" destroy-method="destroy">
      <constructor-arg ref="cacheManager" />
      <constructor-arg ref="scriptParser" />
      <constructor-arg ref="autoLoadConfig" />
      <constructor-arg ref="hessianSerializer" />
    </bean>

### 7.AOP 配置：

    <bean id="cacheInterceptor" class="com.jarvis.cache.aop.aspectj.AspectjAopInterceptor">
      <constructor-arg ref="cacheHandler" />
    </bean>
    <aop:config proxy-target-class="true">
      <!-- 处理 @Cache AOP-->
      <aop:aspect ref="cacheInterceptor">
        <aop:pointcut id="daoCachePointcut" expression="execution(public !void com.jarvis.cache_example.common.dao..*.*(..)) &amp;&amp; @annotation(cache)" />
        <aop:around pointcut-ref="daoCachePointcut" method="proceed" />
      </aop:aspect>

      <!-- 处理 @CacheDelete AOP-->
      <aop:aspect ref="cacheInterceptor" order="1000"><!-- order 参数控制 aop通知的优先级，值越小，优先级越高 ，在事务提交后删除缓存 -->
        <aop:pointcut id="deleteCachePointcut" expression="execution(* com.jarvis.cache_example.common.dao..*.*(..)) &amp;&amp; @annotation(cacheDelete)" />
        <aop:after-returning pointcut-ref="deleteCachePointcut" method="deleteCache" returning="retVal"/>
      </aop:aspect>

      <!-- 处理 @CacheDeleteTransactional AOP-->
      <aop:aspect ref="cacheInterceptor">
        <aop:pointcut id="cacheDeleteTransactional" expression="execution(* com.jarvis.cache_example.common.service..*.*(..)) &amp;&amp; @annotation(cacheDeleteTransactional)" />
        <aop:around pointcut-ref="cacheDeleteTransactional" method="deleteCacheTransactional" />
      </aop:aspect>

    </aop:config>


如果不同的数据，要使用不同的缓存的话，可以通过配置多个AOP来进行共区分。


### 8. 在需要使用缓存操作的方法前增加 @Cache和 @CacheDelete注解

更多的配置可以参照

[autoload-cache-spring-boot-starter](https://github.com/qiujiayu/autoload-cache-spring-boot-starter) 推荐使用这个，[test目录](https://github.com/qiujiayu/autoload-cache-spring-boot-starter/tree/master/src/test)中也有可运行例子。

[Spring 实例代码](https://github.com/qiujiayu/cache-example)，基于Spring XML进行配置

[Spring boot 实例代码](https://github.com/qiujiayu/autoload-cache-spring-boot-starter/tree/master/src/test)

以上配置是基于 Spring 的配置，如果是使用nutz，请参照 [AutoLoadCache-nutz](https://github.com/qiujiayu/AutoLoadCache-nutz) 中的说明。
