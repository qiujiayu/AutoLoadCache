package com.jarvis.cache.memcache;

import java.lang.reflect.Type;

import net.spy.memcached.MemcachedClient;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * memcache缓存管理
 */
public class CachePointCut extends AbstractCacheManager {

    private MemcachedClient memcachedClient;

    public CachePointCut(AutoLoadConfig config, ISerializer<Object> serializer, AbstractScriptParser scriptParser) {
        super(config, serializer, scriptParser);
    }

    @Override
    public void setCache(CacheKeyTO cacheKeyTO, CacheWrapper<Object> result) {
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

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(CacheKeyTO cacheKeyTO, Type returnType) {
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
        return (CacheWrapper<Object>)memcachedClient.get(cacheKey);
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
