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
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用memcache进行缓存
 */
public class CachePointCut implements CacheGeterSeter<Serializable> {

    private static final Logger logger=Logger.getLogger(CachePointCut.class);

    private MemcachedClient memcachedClient;

    private AutoLoadHandler<Serializable> autoLoadHandler;

    public CachePointCut(AutoLoadConfig config) {
        autoLoadHandler=new AutoLoadHandler<Serializable>(this, config);
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

    /**
     * 批量删除缓存
     * @param keys
     */
    public void delete(List<String> keys) {
        try {
            if(null != keys && !keys.isEmpty()) {
                for(String key: keys) {
                    this.delete(key);
                }
            }
        } catch(Exception e) {
        }
    }

    /**
     * 通过组成Key直接删除
     * @param key
     */
    public void delete(String key) {
        try {
            memcachedClient.delete(key);
            autoLoadHandler.resetAutoLoadLastLoadTime(key);
        } catch(Exception e) {
        }
    }

    /**
     * 根据默认缓存Key删除缓存
     * @param cs Class
     * @param method
     * @param arguments
     * @param subKeySpEL
     * @param deleteByPrefixKey 是否批量删除
     */
    public void deleteByDefaultCacheKey(@SuppressWarnings("rawtypes") Class cs, String method, Object[] arguments, String subKeySpEL) {
        String cacheKey=CacheUtil.getDefaultCacheKey(cs.getName(), method, arguments, subKeySpEL);
        this.delete(cacheKey);
    }

    /**
     * 通过Spring EL 表达式，删除缓存
     * @param keySpEL Spring EL表达式
     * @param arguments 参数
     */
    public void deleteDefinedCacheKey(String keySpEL, Object[] arguments) {
        String cacheKey=CacheUtil.getDefinedCacheKey(keySpEL, arguments);
        this.delete(cacheKey);
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
