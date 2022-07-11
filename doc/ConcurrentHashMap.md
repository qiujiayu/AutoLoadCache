### ConcurrentHashMap配置

```xml
<bean id="cacheManager" class="com.jarvis.cache.map.MapCacheManager" init-method="start" destroy-method="destroy">
  <constructor-arg ref="autoLoadConfig" />
  <constructor-arg ref="hessianSerializer" />
</bean>
```

MapCacheManager 中可以配置参数说明：

* needPersist : 是否在持久化:为true时，允许持久化，false，不允许持久化;默认值为true;
* persistFile ： 缓存持久化文件；默认值：linux中为：/tmp/autoload-cache/+namespace+map.cache中，windows中C:/tmp/autoload-cache/+namespace+map.cache
* unpersistMaxSize ： 允许不持久化变更数(当缓存变更数量超过此值才做持久化操作)，默认值为0；
* clearAndPersistPeriod : 清除和持久化的时间间隔,默认值为：60000（1分钟）；

* copyValueOnGet : 从缓存中取数据时，是否克隆：true时，是克隆缓存值，可以避免外界修改缓存值；false，不克隆缓存值，缓存中的数据可能被外界修改，但效率比较高;

* copyValueOnSet : 往缓存中写数据时，是否把克隆后的值放入缓存：true时，是拷贝缓存值，可以避免外界修改缓存值；false，不拷贝缓存值，缓存中的数据可能被外界修改，但效率比较高;

***注意***：通过配置init-method="start"，启动清理缓存线程；通过配置destroy-method="destroy"，释放资源；
使用Map做缓存，虽然可以不需要使用序列化工具进行转换数据，但还需要使用序列化工作进行深度复制。