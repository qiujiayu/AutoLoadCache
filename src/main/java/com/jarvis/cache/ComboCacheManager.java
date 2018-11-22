package com.jarvis.cache;

import java.lang.reflect.Method;
import java.util.Set;

import com.jarvis.cache.annotation.LocalCache;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.to.LocalCacheWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 组合多种缓存管理方案，本地保存短期缓存，远程保存长期缓存
 * 
 * @author gongqin
 * @version 2016年6月8日
 */
@Slf4j
public class ComboCacheManager implements ICacheManager {

    /**
     * 表达式解析器
     */
    private final AbstractScriptParser scriptParser;

    /**
     * 本地缓存实现
     */
    private ICacheManager localCache;

    /**
     * 远程缓存实现
     */
    private ICacheManager remoteCache;

    public ComboCacheManager(ICacheManager localCache, ICacheManager remoteCache, AbstractScriptParser scriptParser) {
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.scriptParser = scriptParser;
    }

    @Override
    public void setCache(CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method, Object[] args)
            throws CacheCenterConnectionException {
        LocalCache lCache = null;
        if (method.isAnnotationPresent(LocalCache.class)) {
            lCache = method.getAnnotation(LocalCache.class);
            setLocalCache(lCache, cacheKey, result, method, args);
            if (lCache.localOnly()) {// 只本地缓存
                return;
            }
        }
        remoteCache.setCache(cacheKey, result, method, args);
    }

    private void setLocalCache(LocalCache lCache, CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method,
            Object[] args) {
        try {
            LocalCacheWrapper<Object> localResult = new LocalCacheWrapper<Object>();
            localResult.setLastLoadTime(System.currentTimeMillis());
            int expire = scriptParser.getRealExpire(lCache.expire(), lCache.expireExpression(), args,
                    result.getCacheObject());
            localResult.setExpire(expire);
            localResult.setCacheObject(result.getCacheObject());

            localResult.setRemoteExpire(result.getExpire());
            localResult.setRemoteLastLoadTime(result.getLastLoadTime());
            localCache.setCache(cacheKey, result, method, args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public CacheWrapper<Object> get(CacheKeyTO key, Method method, Object[] args)
            throws CacheCenterConnectionException {
        String threadName = Thread.currentThread().getName();
        if (threadName.startsWith(AutoLoadHandler.THREAD_NAME_PREFIX)) {// 如果是自动加载线程，则只从远程缓存获取。
            return remoteCache.get(key, method, args);
        }
        LocalCache lCache = null;
        if (method.isAnnotationPresent(LocalCache.class)) {
            CacheWrapper<Object> result = localCache.get(key, method, args);
            lCache = method.getAnnotation(LocalCache.class);
            if (null != result) {
                if (result instanceof LocalCacheWrapper) {
                    LocalCacheWrapper<Object> localResult = (LocalCacheWrapper<Object>) result;
                    CacheWrapper<Object> result2 = new CacheWrapper<Object>();
                    result2.setCacheObject(localResult.getCacheObject());
                    result2.setExpire(localResult.getRemoteExpire());
                    result2.setLastLoadTime(localResult.getRemoteLastLoadTime());
                    return result2;
                } else {
                    return result;
                }
            }
        }
        CacheWrapper<Object> result = remoteCache.get(key, method, args);
        if (null != lCache && result != null) { // 如果取到了则先放到本地缓存里
            setLocalCache(lCache, key, result, method, args);
        }
        return result;
    }

    @Override
    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        localCache.delete(keys);
        remoteCache.delete(keys);
    }
}
