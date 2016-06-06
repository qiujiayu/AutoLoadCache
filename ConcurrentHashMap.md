###ConcurrentHashMap配置

  <bean id="cacheManager" class="com.jarvis.cache.map.CachePointCut" init-method="start" destroy-method="destroy">
    <constructor-arg ref="autoLoadConfig" />
    <constructor-arg ref="hessianSerializer" />
    <constructor-arg ref="scriptParser" />
    <property name="namespace" value="test" />
  </bean>

CachePointCut中可以配置参数说明：

* namespace ： 命名空间，在缓存表达式生成的缓存key中加入此命名空间，达到区分不同业务的数据的作用；
* unpersistMaxSize ： 允许不持久化变更数(当缓存变更数量超过此值才做持久化操作)，默认值为0；
* persistFile ： 缓存持久化文件；默认值：linux中为：/tmp/autoload-cache/+namespace+map.cache中，windows中C:/tmp/autoload-cache/+namespace+map.cache
* needPersist : 是否在持久化:为true时，允许持久化，false，不允许持久化;默认值为true;
* copyValue : 是否拷贝缓存中的值：true时，是拷贝缓存值，可以避免外界修改缓存值；false，不拷贝缓存值，缓存中的数据可能被外界修改，但效率比较高。默认值为false;
* clearAndPersistPeriod : 清除和持久化的时间间隔,默认值为：60000（1分钟）；


***注意***：通过配置init-method="start"，启动清理缓存线程；通过配置destroy-method="destroy"，释放资源；
使用Map做缓存，虽然可以不需要使用序列化工具进行转换数据，但还需要使用序列化工作进行深度复制。