package com.jarvis.cache;


/**
 * @author jiayu.qiu
 */
public interface CacheGeterSeter<T> {

    void setCache(String cacheKey, CacheWrapper<T> result, int expire);

    CacheWrapper<T> get(String key);
}
