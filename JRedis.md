####JRedis配置

Redis配置

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
    
    <bean id="cacheManager" class="com.jarvis.cache.redis.ShardedCachePointCut" destroy-method="destroy">
      <constructor-arg ref="autoLoadConfig" />
      <constructor-arg ref="hessianSerializer" />
      <constructor-arg ref="scriptParser" />
      <property name="shardedJedisPool" ref="shardedJedisPool" />
      <property name="namespace" value="test_hessian" />
    </bean>


ShardedCachePointCut中可以配置参数说明：

* namespace ： 命名空间，在缓存表达式生成的缓存key中加入此命名空间，达到区分不同业务的数据的作用；

* hashExpire：Hash的缓存时长,默认值为0（永久缓存），设置此项大于0时，主要是为了防止一些已经不用的缓存占用内存。如果hashExpire 小于0则使用@Cache中设置的expire值。默认值为-1；

* hashExpireByScript ： 是否通过脚本来设置 Hash的缓存时长；

***注意***：通过配置destroy-method="destroy"，释放资源。