package com.jarvis.cache;

import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存更新
 * 
 * @author jiayu.qiu
 */
public interface ChangeListener {

    /**
     * 缓存更新
     * 
     * @param cacheKey 缓存Key
     * @param newVal 新缓存值
     */
    void update(CacheKeyTO cacheKey, CacheWrapper<Object> newVal);

    /**
     * 缓存删除
     * 
     * @param cacheKey 缓存Key
     */
    void delete(CacheKeyTO cacheKey);
}
