package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Redis缓存操作
 *
 * @author: jiayu.qiu
 */
public interface IRedis extends Closeable {

    void set(final byte[] key, final byte[] value);

    void setex(final byte[] key, final int seconds, final byte[] value);

    void hset(byte[] key, byte[] field, byte[] value);

    void hset(byte[] key, byte[] field, byte[] value, int seconds);

    /**
     * 往缓存写数据
     *
     * @param params 缓存Key 和 缓存数据
     * @throws CacheCenterConnectionException 缓存异常
     */
    void mset(final MSetParam... params);

    byte[] get(byte[] key);

    byte[] hget(final byte[] key, final byte[] field);

    /**
     * 根据缓存Key获得缓存中的数据
     *
     * @param returnType 返回值类型
     * @param keys       缓存keys
     * @return 缓存数据
     * @throws CacheCenterConnectionException 缓存异常
     */
    Map<CacheKeyTO, CacheWrapper<Object>> mget(final Type returnType, final CacheKeyTO... keys) throws Exception;

    /**
     * 批量删除
     *
     * @param keys
     */
    void delete(Set<CacheKeyTO> keys);
}
