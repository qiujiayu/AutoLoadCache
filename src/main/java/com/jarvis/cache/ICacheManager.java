package com.jarvis.cache;

import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存管理
 * @author jiayu.qiu
 */
public interface ICacheManager<T> {

    /**
     * 往缓存写数据
     * @param cacheKey 缓存Key
     * @param result 缓存数据
     * @param expire 缓存时间
     */
    void setCache(String cacheKey, CacheWrapper<T> result, int expire);

    /**
     * 根据缓存Key获得缓存中的数据
     * @param key 缓存key
     * @return 缓存数据
     */
    CacheWrapper<T> get(String key);

    /**
     * 删除缓存
     * @param key 缓存key
     */
    void delete(String key);

    /**
     * 获取自动加载处理器
     * @return 自动加载处理器
     */
    AutoLoadHandler<T> getAutoLoadHandler();

    /**
     * 销毁：关闭线程
     */
    void destroy();
}
