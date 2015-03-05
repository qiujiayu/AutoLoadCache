package com.jarvis.cache.memcache;

import java.io.Serializable;
import java.util.List;

import net.spy.memcached.MemcachedClient;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;

import com.jarvis.cache.AutoLoadHandler;
import com.jarvis.cache.CacheGeterSeter;
import com.jarvis.cache.CacheUtil;
import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用memcache进行缓存
 */
public class CachePointCut implements CacheGeterSeter<Serializable> {

    private static final Logger logger=Logger.getLogger(CachePointCut.class);

    private MemcachedClient memcachedClient;

    private AutoLoadHandler<Serializable> autoLoadHandler;

    public CachePointCut() {
        autoLoadHandler=new AutoLoadHandler<Serializable>(10, this, 10000);
    }

    public CachePointCut(int threadCnt, int maxElement) {
        autoLoadHandler=new AutoLoadHandler<Serializable>(threadCnt, this, maxElement);
    }

    public Serializable controllerPointCut(ProceedingJoinPoint pjp, Cache cache) throws Exception {
        return CacheUtil.proceed(pjp, cache, autoLoadHandler, this);
    }

    @Override
    public void setCache(String cacheKey, CacheWrapper<Serializable> result, int expire) {
        memcachedClient.set(cacheKey, expire, result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Serializable> get(String key) {
        return (CacheWrapper<Serializable>)memcachedClient.get(key);
    }

    public void delete(List<String> keys) {
        try {
            if(null != keys && !keys.isEmpty()) {
                for(String key: keys) {
                    memcachedClient.delete(key);
                }
            }
        } catch(Exception e) {
        }
    }

    public void delete(String key) {
        try {
            memcachedClient.delete(key);
        } catch(Exception e) {
        }
    }

    public AutoLoadHandler<Serializable> getAutoLoadHandler() {
        return autoLoadHandler;
    }

    public void destroy() {
        autoLoadHandler.shutdown();
        autoLoadHandler=null;
        logger.error(CachePointCut.class.getName() + " destroy ... ... ...");
    }

    public MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }

    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient=memcachedClient;
    }

}
