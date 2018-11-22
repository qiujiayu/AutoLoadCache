package com.jarvis.cache.memcache;

import java.lang.reflect.Method;
import java.util.Set;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;

/**
 * memcache缓存管理
 * 
 * @author: jiayu.qiu
 */
@Slf4j
public class MemcachedCacheManager implements ICacheManager {

    private MemcachedClient memcachedClient;

    public MemcachedCacheManager() {
    }

    @Override
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method,
            final Object args[]) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        String hfield = cacheKeyTO.getHfield();
        if (null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        if (result.getExpire() >= 0) {
            memcachedClient.set(cacheKey, result.getExpire(), result);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(final CacheKeyTO cacheKeyTO, Method method, final Object args[])
            throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return null;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.length() == 0) {
            return null;
        }
        String hfield = cacheKeyTO.getHfield();
        if (null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        return (CacheWrapper<Object>) memcachedClient.get(cacheKey);
    }

    @Override
    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        if (null == memcachedClient || null == keys || keys.isEmpty()) {
            return;
        }
        String hfield;
        for(CacheKeyTO cacheKeyTO: keys) {
            if (null == cacheKeyTO) {
                continue;
            }
            String cacheKey = cacheKeyTO.getCacheKey();
            if (null == cacheKey || cacheKey.length() == 0) {
                continue;
            }
            hfield = cacheKeyTO.getHfield();
            if (null != hfield && hfield.length() > 0) {
                throw new RuntimeException("memcached does not support hash cache.");
            }
            try {
                String allKeysPattern = "*";
                if (allKeysPattern.equals(cacheKey)) {
                    memcachedClient.flush();
                } else {
                    memcachedClient.delete(cacheKey);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }

    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

}
