package com.jarvis.cache;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jarvis.cache.annotation.LocalCache;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 组合多种缓存管理方案，本地保存短期缓存，远程保存长期缓存
 * @author gongqin
 * @version 2016年6月8日
 */
public class ComboCacheManager implements ICacheManager {

    private static final Logger logger=LoggerFactory.getLogger(ComboCacheManager.class);

    private final ISerializer<Object> serializer;

    private final ICloner cloner;

    private final AutoLoadConfig config;

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
        this.localCache=localCache;
        this.remoteCache=remoteCache;
        this.serializer=remoteCache.getSerializer();
        this.config=remoteCache.getAutoLoadConfig();
        this.cloner=remoteCache.getCloner();
        this.scriptParser=scriptParser;
    }

    @Override
    public void setCache(CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method, Object[] args) throws CacheCenterConnectionException {
        LocalCache lCache=null;
        if(method.isAnnotationPresent(LocalCache.class)) {
            lCache=method.getAnnotation(LocalCache.class);
        }

        if(null != lCache) {
            setLocalCache(lCache, cacheKey, result, method, args);
            if(lCache.localOnly()) {// 只本地缓存
                return;
            }
        }
        remoteCache.setCache(cacheKey, result, method, args);
    }

    private void setLocalCache(LocalCache lCache, CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method, Object[] args) {
        try {
            CacheWrapper<Object> localResult=(CacheWrapper<Object>)result.clone();
            localResult.setLastLoadTime(System.currentTimeMillis());
            int expire=scriptParser.getRealExpire(lCache.expire(), lCache.expireExpression(), args, result.getCacheObject());
            localResult.setExpire(expire);
            localCache.setCache(cacheKey, result, method, args);
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public CacheWrapper<Object> get(CacheKeyTO key, Method method, Object[] args) throws CacheCenterConnectionException {
        CacheWrapper<Object> result=null;
        LocalCache lCache=null;
        if(method.isAnnotationPresent(LocalCache.class)) {
            result=localCache.get(key, method, args);
            lCache=method.getAnnotation(LocalCache.class);
        }
        if(result == null) {
            result=remoteCache.get(key, method, args);
            if(null != lCache && result != null) { // 如果取到了则先放到本地缓存里
                setLocalCache(lCache, key, result, method, args);
            }
        }
        return result;
    }

    @Override
    public void delete(CacheKeyTO key) throws CacheCenterConnectionException {
        localCache.delete(key);
        remoteCache.delete(key);
    }

    @Override
    public ICloner getCloner() {
        return this.cloner;
    }

    @Override
    public ISerializer<Object> getSerializer() {
        return this.serializer;
    }

    @Override
    public AutoLoadConfig getAutoLoadConfig() {
        return this.config;
    }

}
