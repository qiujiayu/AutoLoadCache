package com.jarvis.cache;

import com.jarvis.cache.to.CacheKeyTO;

/**
 * 缓存更新
 * @author jiayu.qiu
 */
public interface ChangeListener {

    /**
     * 缓存更新
     * @param cacheKey
     */
    void update(CacheKeyTO cacheKey);

    /**
     * 缓存删除
     * @param cacheKey
     */
    void delete(CacheKeyTO cacheKey);
}
