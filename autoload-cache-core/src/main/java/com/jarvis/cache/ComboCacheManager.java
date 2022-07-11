package com.jarvis.cache;

import com.jarvis.cache.annotation.LocalCache;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.to.LocalCacheWrapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

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
    public void setCache(CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method)
            throws CacheCenterConnectionException {
        if (method.isAnnotationPresent(LocalCache.class)) {
            LocalCache lCache = method.getAnnotation(LocalCache.class);
            setLocalCache(lCache, cacheKey, result, method);
            // 只本地缓存
            if (lCache.localOnly()) {
                return;
            }
        }
        remoteCache.setCache(cacheKey, result, method);
    }

    @Override
    public void mset(Method method, Collection<MSetParam> params) throws CacheCenterConnectionException {
        if (method.isAnnotationPresent(LocalCache.class)) {
            LocalCache lCache = method.getAnnotation(LocalCache.class);
            for (MSetParam param : params) {
                if (param == null) {
                    continue;
                }
                setLocalCache(lCache, param.getCacheKey(), param.getResult(), method);
            }
            // 只本地缓存
            if (lCache.localOnly()) {
                return;
            }
        }
        remoteCache.mset(method, params);
    }

    private void setLocalCache(LocalCache lCache, CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method) {
        try {
            LocalCacheWrapper<Object> localResult = new LocalCacheWrapper<Object>();
            localResult.setLastLoadTime(System.currentTimeMillis());
            int expire = scriptParser.getRealExpire(lCache.expire(), lCache.expireExpression(), null,
                    result.getCacheObject());
            localResult.setExpire(expire);
            localResult.setCacheObject(result.getCacheObject());

            localResult.setRemoteExpire(result.getExpire());
            localResult.setRemoteLastLoadTime(result.getLastLoadTime());
            localCache.setCache(cacheKey, result, method);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public CacheWrapper<Object> get(CacheKeyTO key, Method method) throws CacheCenterConnectionException {
        LocalCache lCache = null;
        if (method.isAnnotationPresent(LocalCache.class)) {
            lCache = method.getAnnotation(LocalCache.class);
        }
        String threadName = Thread.currentThread().getName();
        // 如果是自动加载线程，则只从远程缓存获取。
        if (null != lCache && !threadName.startsWith(AutoLoadHandler.THREAD_NAME_PREFIX)) {
            CacheWrapper<Object> result = localCache.get(key, method);
            if (null != result) {
                if (result instanceof LocalCacheWrapper) {
                    LocalCacheWrapper<Object> localResult = (LocalCacheWrapper<Object>) result;
                    return new CacheWrapper<Object>(localResult.getCacheObject(), localResult.getRemoteExpire(), localResult.getRemoteLastLoadTime());
                } else {
                    return result;
                }
            }
        }
        CacheWrapper<Object> result = remoteCache.get(key, method);
        // 如果取到了则先放到本地缓存里
        if (null != lCache && result != null) {
            setLocalCache(lCache, key, result, method);
        }
        return result;
    }

    @Override
    public Map<CacheKeyTO, CacheWrapper<Object>> mget(final Method method, final Type returnType, final Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        LocalCache lCache = null;
        if (method.isAnnotationPresent(LocalCache.class)) {
            lCache = method.getAnnotation(LocalCache.class);
        } else {
            return remoteCache.mget(method, returnType, keys);
        }
        String threadName = Thread.currentThread().getName();
        Map<CacheKeyTO, CacheWrapper<Object>> all;
        Map<CacheKeyTO, CacheWrapper<Object>> remoteResults = null;
        if (!threadName.startsWith(AutoLoadHandler.THREAD_NAME_PREFIX)) {
            all = new HashMap<>(keys.size());
            Map<CacheKeyTO, CacheWrapper<Object>> localResults = localCache.mget(method, returnType, keys);
            if (null != localResults && !localResults.isEmpty()) {
                Iterator<Map.Entry<CacheKeyTO, CacheWrapper<Object>>> iterator = localResults.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<CacheKeyTO, CacheWrapper<Object>> item = iterator.next();
                    CacheWrapper<Object> result = item.getValue();
                    if (result instanceof LocalCacheWrapper) {
                        LocalCacheWrapper<Object> localResult = (LocalCacheWrapper<Object>) result;
                        CacheWrapper<Object> result2 = new CacheWrapper<Object>(localResult.getCacheObject(), localResult.getRemoteExpire(), localResult.getRemoteLastLoadTime());
                        all.put(item.getKey(), result2);
                    } else {
                        all.put(item.getKey(), result);
                    }
                }
            }
            if(all.size() < keys.size()) {
                Set<CacheKeyTO> unCachekeys = new HashSet<>(keys.size() - all.size());
                for(CacheKeyTO key : keys) {
                    if(!all.containsKey(key)) {
                        unCachekeys.add(key);
                    }
                }
                remoteResults = remoteCache.mget(method, returnType, keys);
                if(null != remoteResults && !remoteResults.isEmpty()) {
                    all.putAll(remoteResults);
                }
            }
        } else {
            remoteResults = remoteCache.mget(method, returnType, keys);
            all = remoteResults;
        }

        if(null != remoteResults && !remoteResults.isEmpty()) {
            // 放到本地缓存里
            Iterator<Map.Entry<CacheKeyTO, CacheWrapper<Object>>> iterator = remoteResults.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<CacheKeyTO, CacheWrapper<Object>> item = iterator.next();
                setLocalCache(lCache, item.getKey(), item.getValue(), method);
            }
        }
        return all;
    }

    @Override
    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        localCache.delete(keys);
        remoteCache.delete(keys);
    }
}
