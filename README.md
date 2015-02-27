# AutoLoadCache
---------------------------------------------
现在使用的缓存技术很多，比如*Redis*、 *Memcache* 、 *EhCache*等，甚至还有使用*ConcurrentHashMap* 或 *HashTable* 来实现缓存。但在缓存的使用上，每个人都有自己的实现方式，大部分是直接与业务代码绑定，随着业务的变化，要更换缓存方案时，非常麻烦。接下来我们就使用**AOP + Annotation** 来解决这个问题，同时使用**自动加载机制** 来实现数据“**常驻内存**”。

Spring AOP这几年非常热门，使用也越来越多，但个人建议AOP只用于处理一些辅助的功能（比如：接下来我们要说的缓存），而不能把业务逻辑使用AOP中实现，尤其是在需要“事务”的环境中。

如下图所示：
![Alt 缓存框架](/doc/autoload-cache.png "缓存框架")

AOP拦截到请求后：
>1. 根据请求参数生成Key;
>2. 如果是AutoLoad的，则请求相关参数，封装到AutoLoadTO中，并放到AutoLoadHandler中。
>3. 根据Key去缓存服务器中取数据，如果取到数据，则返回数据，如果没有取到数据，则执行DAO中的方法，获取数据，同时将数据放到缓存中。如果是AutoLoad的，则把最后加载时间，更新到AutoLoadTO中，最后返回数据；如是AutoLoad的请求，每次请求时，都会更新AutoLoadTO中的 最后请求时间。

AutoLoadHandler（自动加载处理器）主要做的事情：当缓存即将过期时，去执行DAO的方法，获取数据，并将数据放到缓存中。为了防止自动加载队列过大，设置了容量限制；同时会将超过一定时间没有用户请求的也会从自动加载队列中移除，把服务器资源释放出来，给真正需要的请求。

**使用自加载的目的:**
>1. 避免在请求高峰时，因为缓存失效，而造成数据库压力无法承受;
>2. 把一些耗时业务得以实现。
>3. 把一些使用非常频繁的数据，使用自动加载，因为这样的数据缓存失效时，最容易造成服务器的压力过大。

**分布式自动加载**

如果将应用部署在多台服务器上，理论上可以认为自动加载队列是由这几台服务器共同完成自动加载任务。比如应用部署在A,B两台服务器上，A服务器自动加载了数据D，（因为两台服务器的自动加载队列是独立的，所以加载的顺序也是一样的），接着有用户从B服务器请求数据D，这时会把数据D的最后加载时间更新给B服务器，这样B服务器就不会重复加载数据D。



##使用方法
###1. 实现com.jarvis.cache.CacheGeterSeter 
下面举个使用Redis做缓存服务器的例子：

    package com.jarvis.example.cache;
    import ... ...
    /**
     * 主要切面，用于拦截数据并调用Redis进行缓存
     */
    @Aspect
    public class CachePointCut implements CacheGeterSeter<Serializable> {

        private static final Logger logger=Logger.getLogger(CachePointCut.class);

        private AutoLoadHandler<Serializable> autoLoadHandler;

        private static List<RedisTemplate<String, Serializable>> redisTemplateList;

        public CachePointCut() {
            autoLoadHandler=new AutoLoadHandler<Serializable>(10, this, 20000);
        }

        @Pointcut(value="execution(public !void com.jarvis.example.dao..*.*(..)) && @annotation(cahce)", argNames="cahce")
        public void daoCachePointcut(Cache cahce) {
            logger.error("----------------------init daoCachePointcut()--------------------");
        }

        @Around(value="daoCachePointcut(cahce)", argNames="pjp, cahce")
        public Object controllerPointCut(ProceedingJoinPoint pjp, Cache cahce) throws Exception {
            return CacheUtil.proceed(pjp, cahce, autoLoadHandler, this);
        }

        public static RedisTemplate<String, Serializable> getRedisTemplate(String key) {
            if(null == redisTemplateList || redisTemplateList.isEmpty()) {
                return null;
            }
            int hash=Math.abs(key.hashCode());
            Integer clientKey=hash % redisTemplateList.size();
            RedisTemplate<String, Serializable> redisTemplate=redisTemplateList.get(clientKey);
            return redisTemplate;
        }

        @Override
        public void setCache(final String cacheKey, final CacheWrapper<Serializable> result, final int expire) {
            try {
                final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
                redisTemplate.execute(new RedisCallback<Object>() {

                    @Override
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);
                        JdkSerializationRedisSerializer serializer=(JdkSerializationRedisSerializer)redisTemplate.getValueSerializer();
                        byte[] val=serializer.serialize(result);
                        connection.set(key, val);
                        connection.expire(key, expire);
                        return null;
                    }
                });
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        @Override
        public CacheWrapper<Serializable> get(final String cacheKey) {
            CacheWrapper<Serializable> res=null;
            try {
                final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
                res=redisTemplate.execute(new RedisCallback<CacheWrapper<Serializable>>() {

                    @Override
                    public CacheWrapper<Serializable> doInRedis(RedisConnection connection) throws DataAccessException {
                        byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);
                        byte[] value=connection.get(key);
                        if(null != value && value.length > 0) {
                            JdkSerializationRedisSerializer serializer=
                                (JdkSerializationRedisSerializer)redisTemplate.getValueSerializer();
                            @SuppressWarnings("unchecked")
                            CacheWrapper<Serializable> res=(CacheWrapper<Serializable>)serializer.deserialize(value);
                            return res;
                        }
                        return null;
                    }
                });
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            return res;
        }

        /**
         * 删除缓存
         * @param cs Class
         * @param method
         * @param arguments
         * @param subKeySpEL
         * @param deleteByPrefixKey 是否批量删除
         */
        public static void delete(@SuppressWarnings("rawtypes") Class cs, String method, Object[] arguments, String subKeySpEL,
            boolean deleteByPrefixKey) {
            try {
                if(deleteByPrefixKey) {
                    final String cacheKey=CacheUtil.getCacheKeyPrefix(cs.getName(), method, arguments, subKeySpEL) + "*";
                    for(final RedisTemplate<String, Serializable> redisTemplate : redisTemplateList){
                        redisTemplate.execute(new RedisCallback<Object>() {
                            @Override
                            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                                byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);
                                Set<byte[]> keys=connection.keys(key);
                                if(null != keys && keys.size() > 0) {
                                    byte[][] keys2=new byte[keys.size()][];
                                    keys.toArray(keys2);
                                    connection.del(keys2);
                                }
                                return null;
                            }
                        });
                    }

                } else {
                    final String cacheKey=CacheUtil.getCahcaheKey(cs.getName(), method, arguments, subKeySpEL);
                    final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
                    redisTemplate.execute(new RedisCallback<Object>() {

                        @Override
                        public Object doInRedis(RedisConnection connection) throws DataAccessException {
                            byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);

                            connection.del(key);

                            return null;
                        }
                    });
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        public AutoLoadHandler<Serializable> getAutoLoadHandler() {
            return autoLoadHandler;
        }

        public void destroy() {
            autoLoadHandler.shutdown();
            autoLoadHandler=null;
        }

        public List<RedisTemplate<String, Serializable>> getRedisTemplateList() {
            return redisTemplateList;
        }

        public void setRedisTemplateList(List<RedisTemplate<String, Serializable>> redisTemplateList) {
            CachePointCut.redisTemplateList=redisTemplateList;
        }

    }

从上面的代码可以看出，对缓存的操作，还是由业务系统自己来实现的，我们只是对AOP拦截到的ProceedingJoinPoint，进行做一些处理。

java代码实现后，接下来要在spring中进行相关的配置：

    <aop:aspectj-autoproxy proxy-target-class="true"/>
    <bean id="cachePointCut" class="com.jarvis.example.cache.CachePointCut" destroy-method="destroy">
        <property name="redisTemplateList">
            <list>
                <ref bean="redisTemplate1"/>
                <ref bean="redisTemplate2"/>
            </list>
        </property>
    </bean>


###2. 将需要使用缓存的方法中增加@Cache注解

    package com.jarvis.example.dao;
    import ... ...
    public class UserDAO {
        @Cache(expire=600, autoload=true, requestTimeout=72000)
        public List<UserTO> getUserList(... ...) {
            ... ...
        }
    }


