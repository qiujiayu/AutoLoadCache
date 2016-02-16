package com.jarvis.cache.memcache;

import net.spy.memcached.MemcachedClient;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用memcache进行缓存
 */
public class CachePointCut extends AbstractCacheManager {

    private MemcachedClient memcachedClient;

    public CachePointCut(AutoLoadConfig config) {
        super(config);
    }

    @Override
    public void setCache(CacheKeyTO cacheKeyTO, CacheWrapper result) {
        if(null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        String hfield=cacheKeyTO.getHfield();
        if(null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        memcachedClient.set(cacheKey, result.getExpire(), result);
    }

    @Override
    public CacheWrapper get(CacheKeyTO cacheKeyTO) {
        if(null == cacheKeyTO) {
            return null;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return null;
        }
        String hfield=cacheKeyTO.getHfield();
        if(null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        return (CacheWrapper)memcachedClient.get(cacheKey);
    }

    /**
     * 通过组成Key直接删除
     * @param cacheKeyTO 缓存Key
     */
    @Override
    public void delete(CacheKeyTO cacheKeyTO) {
        if(null == memcachedClient || null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        String hfield=cacheKeyTO.getHfield();
        if(null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        try {
            if("*".equals(cacheKey)) {
                memcachedClient.flush();
            } else {
                memcachedClient.delete(cacheKey);
            }
            this.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKeyTO);
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
