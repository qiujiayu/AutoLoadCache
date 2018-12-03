package com.jarvis.cache.memcache;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.MSetParam;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.isEmpty()) {
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

    @Override
    public void mset(final Method method, final MSetParam... params) throws CacheCenterConnectionException {
        if (null == params || params.length == 0) {
            return;
        }
        for (MSetParam param : params) {
            this.setCache(param.getCacheKey(), param.getResult(), method);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(final CacheKeyTO cacheKeyTO, Method method) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return null;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.isEmpty()) {
            return null;
        }
        String hfield = cacheKeyTO.getHfield();
        if (null != hfield && hfield.length() > 0) {
            throw new RuntimeException("memcached does not support hash cache.");
        }
        return (CacheWrapper<Object>) memcachedClient.get(cacheKey);
    }

    @Override
    public Map<CacheKeyTO, CacheWrapper<Object>> mget(final Method method, final CacheKeyTO... keys) throws CacheCenterConnectionException {
        if (null == keys || keys.length == 0) {
            return null;
        }
        String[] k = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            k[i] = keys[i].getCacheKey();
        }
        Map<String, Object> values = memcachedClient.getBulk(k);
        if (null == values || values.isEmpty()) {
            return null;
        }

        int len = keys.length;
        Map<CacheKeyTO, CacheWrapper<Object>> res = new HashMap<>(len);
        for (int i = 0; i < len; i++) {
            res.put(keys[i], (CacheWrapper<Object>) values.get(keys[i].getCacheKey()));
        }
        return res;
    }

    @Override
    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        if (null == memcachedClient || null == keys || keys.isEmpty()) {
            return;
        }
        String hfield;
        for (CacheKeyTO cacheKeyTO : keys) {
            if (null == cacheKeyTO) {
                continue;
            }
            String cacheKey = cacheKeyTO.getCacheKey();
            if (null == cacheKey || cacheKey.isEmpty()) {
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
