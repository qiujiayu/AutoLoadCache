#### JRedis配置

Redis配置(基于一致性哈希算法)

    <!-- Jedis 连接池配置 -->
    <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
      <property name="maxTotal" value="2000" />
      <property name="maxIdle" value="100" />
      <property name="minIdle" value="50" />
      <property name="maxWaitMillis" value="2000" />
      <property name="testOnBorrow" value="false" />
      <property name="testOnReturn" value="false" />
      <property name="testWhileIdle" value="false" />
    </bean>

    <bean id="shardedJedisPool" class="redis.clients.jedis.ShardedJedisPool">
      <constructor-arg ref="jedisPoolConfig" />
      <constructor-arg>
        <list>
          <bean class="redis.clients.jedis.JedisShardInfo">
          <constructor-arg value="${redis1.host}" />
          <constructor-arg type="int" value="${redis1.port}" />
          <constructor-arg value="instance:01" />
        </bean>
        <bean class="redis.clients.jedis.JedisShardInfo">
          <constructor-arg value="${redis2.host}" />
          <constructor-arg type="int" value="${redis2.port}" />
          <constructor-arg value="instance:02" />
        </bean>
        <bean class="redis.clients.jedis.JedisShardInfo">
          <constructor-arg value="${redis3.host}" />
          <constructor-arg type="int" value="${redis3.port}" />
          <constructor-arg value="instance:03" />
        </bean>
        </list>
      </constructor-arg>
    </bean>
    
    <bean id="cacheManager" class="com.jarvis.cache.redis.ShardedJedisCacheManager">
      <constructor-arg ref="autoLoadConfig" />
      <constructor-arg ref="hessianSerializer" />
      <constructor-arg ref="scriptParser" />
      <property name="shardedJedisPool" ref="shardedJedisPool" />
    </bean>

ShardedJedisCacheManager 中可以配置参数说明：


* hashExpire：Hash的缓存时长：等于0时永久缓存；大于0时，主要是为了防止一些已经不用的缓存占用内存;hashExpire小于0时，则使用@Cache中设置的expire值（默认值为-1）；

* hashExpireByScript ： 是否通过脚本来设置 Hash的缓存时长；

另外也可以使用 com.jarvis.cache.redis.JedisClusterCacheManager 实现对JedisCluster的操作。

