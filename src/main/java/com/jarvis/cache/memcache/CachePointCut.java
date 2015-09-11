package com.jarvis.cache.memcache;

import java.io.Serializable;
import java.util.List;

import net.spy.memcached.MemcachedClient;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.CacheUtil;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用memcache进行缓存
 */
public class CachePointCut extends AbstractCacheManager<Serializable> {

    private MemcachedClient memcachedClient;

    private String namespace;

    public CachePointCut(AutoLoadConfig config) {
        super(config);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace=namespace;
    }

    private String appendNamespace(String cacheKey) {
        if(null != namespace && namespace.length() > 0) {
            return namespace + ":" + cacheKey;
        }
        return cacheKey;
    }

    @Override
    public void setCache(String cacheKey, CacheWrapper<Serializable> result, int expire) {
        result.setLastLoadTime(System.currentTimeMillis());
        cacheKey=appendNamespace(cacheKey);
        memcachedClient.set(cacheKey, expire, result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Serializable> get(String cacheKey) {
        cacheKey=appendNamespace(cacheKey);
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
        cacheKey=appendNamespace(cacheKey);
        try {
            memcachedClient.delete(cacheKey);
            this.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKey);
        } catch(Exception e) {
        }
    }

    /**
     * 根据默认缓存Key删除缓存
     * @param cs Class
     * @param method 方法名
     * @param arguments 参数
     * @param subKeySpEL SpringEL表达式，arguments 在SpringEL表达式中的名称为args，第一个参数为#args[0],第二个为参数为#args[1]，依此类推。
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

    /**
     * 批量删除缓存
     * @param keys 缓存Key列表
     */
    public void delete(List<String> keys) {
        try {
            if(null != keys && !keys.isEmpty()) {
                for(String cacheKey: keys) {
                    this.delete(cacheKey);
                }
            }
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
