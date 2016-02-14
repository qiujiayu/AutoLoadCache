package com.jarvis.cache;

import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存管理
 * @author jiayu.qiu
 */
public interface ICacheManager {

    /**
     * 往缓存写数据
     * @param cacheKey 缓存Key
     * @param result 缓存数据
     */
    void setCache(CacheKeyTO cacheKey, CacheWrapper result);

    /**
     * 根据缓存Key获得缓存中的数据
     * @param key 缓存key
     * @return 缓存数据
     */
    CacheWrapper get(CacheKeyTO key);

    /**
     * 删除缓存
     * @param key 缓存key
     */
    void delete(CacheKeyTO key);

    /**
     * 获取自动加载处理器
     * @return 自动加载处理器
     */
    AutoLoadHandler getAutoLoadHandler();

    /**
     * 销毁：关闭线程
     */
    void destroy();
}
