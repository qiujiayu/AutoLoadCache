### Memcache配置

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

    <bean id="cacheManager" class="com.jarvis.cache.memcache.MemcachedCacheManager">
      <constructor-arg ref="autoLoadConfig" />
      <constructor-arg ref="hessianSerializer" />
      <property name="memcachedClient", ref="memcachedClient" />
    </bean>

    <bean id="cacheHandler" class="com.jarvis.cache.CacheHandler" destroy-method="destroy">
      <constructor-arg ref="cacheManager" />
      <constructor-arg ref="scriptParser" />
    </bean>

