package com.jarvis.cache;

import com.jarvis.cache.to.CacheWrapper;

/**
 * @author jiayu.qiu
 */
public interface CacheGeterSeter<T> {

    void setCache(String cacheKey, CacheWrapper<T> result, int expire);

    CacheWrapper<T> get(String key);
}
