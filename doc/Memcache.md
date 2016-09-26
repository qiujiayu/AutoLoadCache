###Memcache配置

    <bean id="memcachedClient" class="net.spy.memcached.spring.MemcachedClientFactoryBean">
        <property name="servers" value="192.138.11.165:11211,192.138.11.166:11211" />
        <property name="protocol" value="BINARY" />
        <property name="transcoder">
            <bean class="net.spy.memcached.transcoders.SerializingTranscoder">
                <property name="compressionThreshold" value="1024" />
            </bean>
        </property>
        <property name="opTimeout" value="2000" />
        <property name="timeoutExceptionThreshold" value="1998" />
        <property name="hashAlg">
            <value type="net.spy.memcached.DefaultHashAlgorithm">KETAMA_HASH</value>
        </property>
        <property name="locatorType" value="CONSISTENT" />
        <property name="failureMode" value="Redistribute" />
        <property name="useNagleAlgorithm" value="false" />
    </bean>

    <bean id="hessianSerializer" class="com.jarvis.cache.serializer.HessianSerializer" />
    <bean id="cacheManager" class="com.jarvis.cache.memcache.CachePointCut" destroy-method="destroy">
      <constructor-arg ref="autoLoadConfig" />
      <constructor-arg ref="hessianSerializer" />
      <constructor-arg ref="scriptParser" />
      <property name="memcachedClient", ref="memcachedClient" />
      <property name="namespace" value="test" />
    </bean>


CachePointCut中可以配置参数说明：

* namespace ： 命名空间，在缓存表达式生成的缓存key中加入此命名空间，达到区分不同业务的数据的作用；


***注意***：通过配置destroy-method="destroy"，释放资源。