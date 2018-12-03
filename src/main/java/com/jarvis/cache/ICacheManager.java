package com.jarvis.cache;

import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 缓存管理
 *
 * @author jiayu.qiu
 */
public interface ICacheManager {

    /**
     * 当过期时间为0时，说明永不过期
     */
    int NEVER_EXPIRE = 0;

    /**
     * 往缓存写数据
     *
     * @param cacheKey 缓存Key
     * @param result   缓存数据
     * @param method   Method
     * @throws CacheCenterConnectionException 缓存异常
     */
    void setCache(final CacheKeyTO cacheKey, final CacheWrapper<Object> result, final Method method) throws CacheCenterConnectionException;

    /**
     * 往缓存写数据
     *
     * @param method Method
     * @param params 缓存Key 和 缓存数据
     * @throws CacheCenterConnectionException 缓存异常
     */
    void mset(final Method method, final MSetParam... params) throws CacheCenterConnectionException;

    /**
     * 根据缓存Key获得缓存中的数据
     *
     * @param key    缓存key
     * @param method Method
     * @return 缓存数据
     * @throws CacheCenterConnectionException 缓存异常
     */
    CacheWrapper<Object> get(final CacheKeyTO key, final Method method) throws CacheCenterConnectionException;

    /**
     * 根据缓存Key获得缓存中的数据
     *
     * @param method Method
     * @param keys   缓存keys
     * @return 缓存数据 返回值必须与keys 一一对应
     * @throws CacheCenterConnectionException 缓存异常
     */
    Map<CacheKeyTO, CacheWrapper<Object>> mget(final Method method, final CacheKeyTO... keys) throws CacheCenterConnectionException;

    /**
     * 删除缓存
     *
     * @param keys 缓存keys
     * @throws CacheCenterConnectionException 缓存异常
     */
    void delete(final Set<CacheKeyTO> keys) throws CacheCenterConnectionException;

}
