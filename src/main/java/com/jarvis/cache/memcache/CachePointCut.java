package com.jarvis.cache.memcache;

import java.io.Serializable;

import net.spy.memcached.MemcachedClient;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.to.AutoLoadConfig;
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
    public void setCache(String cacheKey, CacheWrapper<Serializable> result, int expire) {
        result.setLastLoadTime(System.currentTimeMillis());
        memcachedClient.set(cacheKey, expire, result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Serializable> get(String cacheKey) {
        return (CacheWrapper<Serializable>)memcachedClient.get(cacheKey);
    }

    /**
     * 通过组成Key直接删除
     * @param cacheKey 缓存Key
     */
    @Override
    public void delete(String cacheKey) {
        if(null == memcachedClient || null == cacheKey) {
            return;
        }
        try {
            memcachedClient.delete(cacheKey);
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
