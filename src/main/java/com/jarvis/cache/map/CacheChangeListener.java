package com.jarvis.cache.map;

/**
 * 缓存变更监听器
 * 
 * @author jiayu.qiu
 */
public interface CacheChangeListener {

    /**
     * 只变更一条记录
     */
    void cacheChange();

    /**
     * 变更多条记录
     * 
     * @param cnt 变更数量
     */
    void cacheChange(int cnt);
}
