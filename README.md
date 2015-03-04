# AutoLoadCache
---------------------------------------------
现在使用的缓存技术很多，比如*Redis*、 *Memcache* 、 *EhCache*等，甚至还有使用*ConcurrentHashMap* 或 *HashTable* 来实现缓存。但在缓存的使用上，每个人都有自己的实现方式，大部分是直接与业务代码绑定，随着业务的变化，要更换缓存方案时，非常麻烦。接下来我们就使用**AOP + Annotation** 来解决这个问题，同时使用**自动加载机制** 来实现数据“**常驻内存**”。

Spring AOP这几年非常热门，使用也越来越多，但个人建议AOP只用于处理一些辅助的功能（比如：接下来我们要说的缓存），而不能把业务逻辑使用AOP中实现，尤其是在需要“事务”的环境中。

如下图所示：
![Alt 缓存框架](/doc/autoload-cache.png "缓存框架")

AOP拦截到请求后：
>1. 根据请求参数生成Key，后面我们会对生成Key的规则，进一步说明；
>2. 如果是AutoLoad的，则请求相关参数，封装到AutoLoadTO中，并放到AutoLoadHandler中。
>3. 根据Key去缓存服务器中取数据，如果取到数据，则返回数据，如果没有取到数据，则执行DAO中的方法，获取数据，同时将数据放到缓存中。如果是AutoLoad的，则把最后加载时间，更新到AutoLoadTO中，最后返回数据；如是AutoLoad的请求，每次请求时，都会更新AutoLoadTO中的 最后请求时间。
>4. 为了减少并发，增加等待机制：如果多个用户同时取一个数据，那么先让第一个用户去DAO取数据，其它用户则等待其返回后，去缓存中获取，尝试一定次数后，如果还没获取到，再去DAO中取数据。

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
     * 缓存切面，用于拦截数据并调用Redis进行缓存操作
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
            logger.info("----------------------init daoCachePointcut()--------------------");
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
                    final String cacheKey=CacheUtil.getDefaultCacheKeyPrefix(cs.getName(), method, arguments, subKeySpEL) + "*";
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
                    final String cacheKey=CacheUtil.getDefaultCacheKey(cs.getName(), method, arguments, subKeySpEL);
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


###2. 将需要使用缓存的方法前增加@Cache注解

    package com.jarvis.example.dao;
    import ... ...
    public class UserDAO {
        @Cache(expire=600, autoload=true, requestTimeout=72000)
        public List<UserTO> getUserList(... ...) {
            ... ...
        }
    }

##缓存Key的生成

1. 使用Spring EL 表达式自定义缓存Key:CacheUtil.getDefinedCacheKey(String keySpEL, Object[] arguments)

    例如： @Cache(expire=600, key="'goods'+#args[0]")

2. 默认生成缓存Key的方法：CacheUtil.getDefaultCacheKey(String className, String method, Object[] arguments, String subKeySpEL)

 * **className** 类名称
 * **method** 方法名称
 * **arguments** 参数
 * **subKeySpEL** SpringEL表达式

    生成的Key格式为：{类名称}.{方法名称}{.SpringEL表达式运算结果}:{参数值的Hash字符串}。

    当@Cache中不设置key值时，使用默认方式生成缓存Key

建议使用默认生成缓存Key的方法，能减少一些维护工作。

###subKeySpEL 使用说明

根据业务的需要，将缓存Key进行分组。举个例子，商品的评论列表：

    package com.jarvis.example.dao;
    import ... ...
    public class GoodsCommentDAO{
        @Cache(expire=600, subKeySpEL="#args[0]", autoload=true, requestTimeout=18000)
        public List<CommentTO> getCommentListByGoodsId(Long goodsId, int pageNo, int pageSize) {
            ... ...
        }
    }

如果商品Id为：100，那么生成缓存Key格式为:com.jarvis.example.dao.GoodsCommentDAO.getCommentListByGoodsId.100:xxxx
在Redis中，能精确删除商品Id为100的评论列表，执行命令即可：
del com.jarvis.example.dao.GoodsCommentDAO.getCommentListByGoodsId.100:*

SpringEL表达式使用起来确实非常方便，如果需要，@Cache中的expire，requestTimeout以及autoload参数都可以用SpringEL表达式来动态设置，但使用起来就变得复杂，所以我们没有这样做。

###数据实时性

上面商品评论的例子中，如果用户发表了评论，要立即显示该如何来处理？

比较简单的方法就是，在发表评论成功后，立即把缓存中的数据也清除，这样就可以了。

    package com.jarvis.example.dao;
    import ... ...
    public class GoodsCommentDAO{
        @Cache(expire=600, subKeySpEL="#args[0]", autoload=true, requestTimeout=18000)
        public List<CommentTO> getCommentListByGoodsId(Long goodsId, int pageNo, int pageSize) {
            ... ...
        }
        public void addComment(Long goodsId, String comment) {
            ... ...// 省略添加评论代码
            deleteCache(goodsId);
        }
        private void deleteCache(Long goodsId) {
            Object arguments[]=new Object[]{goodsId};
            CachePointCut.delete(this.getClass(), "getCommentListByGoodsId", arguments, "#args[0]", true);
        }
    }

###@Cache

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Cache {

        /**
         * 缓存的过期时间，单位：秒
         */
        int expire();

        /**
         * 自定义缓存Key,如果不设置使用系统默认生成缓存Key的方法
         * @return
         */
        String key() default "";
        
        /**
         * 是否启用自动加载缓存
         * @return
         */
        boolean autoload() default false;

        /**
         * 当autoload为true时，缓存数据在 requestTimeout 秒之内没有使用了，就不进行自动加载数据,如果requestTimeout为0时，会一直自动加载
         * @return
         */
        long requestTimeout() default 36000L;
        
        /**
         * 使用SpEL，将缓存key，根据业务需要进行二次分组
         * @return
         */
        String subKeySpEL() default "";
        /**
         * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存，例如:"#args[0]==1",当第一个参数值为1时，才进缓存。
         * @return
         */
        String condition() default "";
    }


##注意事项

###1. 当@Cache中 **autoload** 设置为 **ture** 时，对应方法的参数必须都是Serializable的。
AutoLoadHandler中需要缓存通过**深度复制**后的参数。

###2. 参数中只设置必要的属性值，在DAO中用不到的属性值尽量不要设置，这样能避免生成不同的缓存Key，降低缓存的使用率。
例如：

        public CollectionTO<AccountTO> getAccountByCriteria(AccountCriteriaTO criteria)  {
            List<AccountTO> list=null;
            PaginationTO paging=criteria.getPaging();
            if(null != paging && paging.getPageNo() > 0 && paging.getPageSize() > 0) {// 如果需要分页查询，先查询总数
                criteria.setPaging(null);// 减少缓存KEY的变化，在查询记录总数据时，不用设置分页相关的属性值
                Integer recordCnt=accountDAO.getAccountCntByCriteria(criteria);
                if(recordCnt > 0) {
                    criteria.setPaging(paging);
                    paging.setRecordCnt(recordCnt);
                    list=accountDAO.getAccountByCriteria(criteria);
                }
                return new CollectionTO<AccountTO>(list, recordCnt, criteria.getPaging().getPageSize());
            } else {
                list=accountDAO.getAccountByCriteria(criteria);
                return new CollectionTO<AccountTO>(list, null != list ? list.size() : 0, 0);
            }
        }

###3. 注意AOP失效的情况;
例如：

        TempDAO {

            public Object a() {
                return b().get(0);
            }

            @Cache(expire=600)
            public List<Object> b(){
                return ... ...;
            }
        }

通过 new TempDAO().a() 调用b方法时，AOP失效，也无法进行缓存相关操作。

###4. 自动加载缓存时，不能在缓存方法内叠加查询参数值;
例如：

        @Cache(expire=600, autoload=true)
        public List<AccountTO> getDistinctAccountByPlayerGet(AccountCriteriaTO criteria) {
            List<AccountTO> list;
            int count=criteria.getPaging().getThreshold() ;
            // 查预设查询数量的10倍
            criteria.getPaging().setThreshold(count * 10);
            … …
        }

因为自动加载时，AutoLoadHandler 缓存了查询参数，执行自动加载时，每次执行时 threshold 都会乘以10，这样threshold的值就会越来越大。

###5. 当方法返回值类型改变了怎么办？

在代码重构时，可能会出现改方法返回值类型的情况，而参数不变的情况，那上线部署时，可能会从缓存中取到旧数据类型的数据，可以通过以下方法处理：

* 上线后，快速清理缓存中的数据；
* 在CacheGeterSeter的实现类中统一加个version；
* 在@Cache中加version（未实现）。

###6. 对于一些比较耗时的方法尽量使用自动加载。

###7. 对于查询条件变化比较剧烈的，不要使用自动加载机制。
比如，根据用户输入的关键字进行搜索数据的方法，不建议使用自动加载。

##在事务环境中，如何减少“脏读”

1. 不要从缓存中取数据，然后应用到修改数据的SQL语句中

2. 在事务完成后，再删除相关的缓存

在事务开始时，用一个ThreadLocal记录一个HashSet，在更新数据方法执行完时，把要删除缓存的相关参数封装成在一个Bean中，放到这个HashSet中，在事务完成时，遍历这个HashSet，然后删除相关缓存。

大部分情况，只要做到第1点就可以了，因为保证数据库中的数据准确才是最重要的。因为这种“脏读”的情况只能减少出现的概率，不能完成解决。一般只有在非常高并发的情况才有可能发生。就像12306，在查询时告诉你还有车票，但最后支付时不一定会有。

##使用规范

1. 将调接口或数据库中取数据，**封装在DAO层**，不能什么地方都有调接口的方法。
2. 自动加载缓存时，**不能**在缓存方法内**叠加（或减）**查询条件值，但允许设置值。
3. DAO层内部，没使用@Cache的方法，不能调用加了@Cache的方法，避免AOP失效。
4. 因缓存Key是方法参数转为字符串获得的，为了避免生成的Key不同，**尽量只设置必要的参数及属性**，也便于**反向定位**。
5. 对于比较大的系统，要进行**模块化设计**，这样可以将自动加载，均分到各个模块中。

##为什么要使用自动加载机制？

首先我们想一下系统的瓶颈在哪里？

1. 在高并发的情况下数据库性能极差，即使查询语句的性能很高；如果没有自动加载机制的话，在当缓存过期时，访问洪峰到来时，很容易就使数据压力大增。

2. 往缓存写数据与从缓存读数据相比，效率也差很多，因为写缓存时需要分配内存等操作。使用自动加载，可以减少同时往缓存写数据的情况，同时也能提升缓存服务器的吞吐量。
3. 还有一些比较耗时的业务。

##如何减少DAO层并发

1. 使用缓存；
2. 使用自动加载机制；“写”数据往往比读数据性能要差，使用自动加载也能减少写并发。
3. 从DAO层加载数据时，增加等待机制（拿来主义）：如果有多个请求同时请求同一个数据，会先让其中一个请求去取数据，其它的请求则等待它的数据。

##可扩展性及维护性

1. 通过AOP实现缓存与业务逻辑的解耦；如果要实时显示数据，还是会有点耦合。
2. 非常方便更换缓存服务器或缓存实现（比如：从Memcache换成Redis）；
3. 非常方便增减缓存服务器（如：增加Redis的节点数）；
4. 非常方便增加或去除缓存，方便测试期间排查问题；


