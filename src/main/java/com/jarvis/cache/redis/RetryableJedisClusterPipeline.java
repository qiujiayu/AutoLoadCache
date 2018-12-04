package com.jarvis.cache.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.exceptions.JedisMovedDataException;

import java.lang.reflect.Field;
import java.util.List;

@Slf4j
public abstract class RetryableJedisClusterPipeline {

    /**
     * 部分字段没有对应的获取方法，只能采用反射来做
     * 也可以去继承JedisCluster和JedisSlotBasedConnectionHandler来提供访问接口
     **/
    private static final Field FIELD_CONNECTION_HANDLER;
    private static final Field FIELD_CACHE;

    static {
        FIELD_CONNECTION_HANDLER = getField(BinaryJedisCluster.class, "connectionHandler");
        FIELD_CACHE = getField(JedisClusterConnectionHandler.class, "cache");
    }

    private final JedisSlotBasedConnectionHandler connectionHandler;

    private final JedisClusterInfoCache clusterInfoCache;

    private int maxAttempts = 1;

    public RetryableJedisClusterPipeline(JedisCluster jedisCluster) {
        connectionHandler = getValue(jedisCluster, FIELD_CONNECTION_HANDLER);
        clusterInfoCache = getValue(connectionHandler, FIELD_CACHE);
    }

    public abstract void execute(JedisClusterPipeline pipeline) throws Exception;

    /**
     * 同步读取所有数据. 与syncAndReturnAll()相比，sync()只是没有对数据做反序列化
     * @throws Exception redis 异常
     */
    public void sync() throws Exception {
        try {
            JedisClusterPipeline pipeline = new JedisClusterPipeline(clusterInfoCache);
            execute(pipeline);
            pipeline.sync();
        } catch (JedisMovedDataException jre) {
            // if MOVED redirection occurred, rebuilds cluster's slot cache,
            // recommended by Redis cluster specification
            connectionHandler.renewSlotCache();
            if (maxAttempts > 0) {
                maxAttempts--;
                sync();
                return;
            }
            throw jre;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 同步读取所有数据 并按命令顺序返回一个列表
     *
     * @return 按照命令的顺序返回所有的数据
     * @throws Exception redis 异常
     */
    public List<Object> syncAndReturnAll() throws Exception {
        try {
            JedisClusterPipeline pipeline = new JedisClusterPipeline(clusterInfoCache);
            execute(pipeline);
            return pipeline.syncAndReturnAll();
        } catch (JedisMovedDataException jre) {
            // if MOVED redirection occurred, rebuilds cluster's slot cache,
            // recommended by Redis cluster specification
            connectionHandler.renewSlotCache();
            if (maxAttempts > 0) {
                maxAttempts--;
                return syncAndReturnAll();
            }
            throw jre;
        } catch (Exception ex) {
            throw ex;
        }
    }

    private static Field getField(Class<?> cls, String fieldName) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException("cannot find or access field '" + fieldName + "' from " + cls.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <T> T getValue(Object obj, Field field) {
        try {
            return (T) field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("get value fail", e);
            throw new RuntimeException(e);
        }
    }
}
