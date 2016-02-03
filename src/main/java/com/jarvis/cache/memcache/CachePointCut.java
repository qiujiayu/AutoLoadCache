package com.jarvis.cache.memcache;

import java.io.Serializable;

import net.spy.memcached.MemcachedClient;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用memcache进行缓存
 */
public class CachePointCut extends AbstractCacheManager<Serializable> {

    private MemcachedClient memcachedClient;

    public CachePointCut(AutoLoadConfig config) {
        super(config);
    }

    @Override
    public void setCache(CacheKeyTO cacheKey, CacheWrapper<Serializable> result) {
        result.setLastLoadTime(System.currentTimeMillis());
        String hfield=cacheKey.getHfield();
        if(null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        memcachedClient.set(cacheKey.getCacheKey(), result.getExpire(), result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Serializable> get(CacheKeyTO cacheKey) {
        String hfield=cacheKey.getHfield();
        if(null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        return (CacheWrapper<Serializable>)memcachedClient.get(cacheKey.getCacheKey());
    }

    /**
     * 通过组成Key直接删除
     * @param cacheKey 缓存Key
     */
    @Override
    public void delete(CacheKeyTO cacheKey) {
        if(null == memcachedClient || null == cacheKey) {
            return;
        }
        String hfield=cacheKey.getHfield();
        if(null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        try {
            memcachedClient.delete(cacheKey.getCacheKey());
            this.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKey);
        } catch(Exception e) {
        }
    }

    public MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }

    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient=memcachedClient;
    }

}
