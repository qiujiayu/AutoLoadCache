package com.jarvis.cache.to;

import java.io.Serializable;

/**
 * 对缓存数据进行封装
 * @author jiayu.qiu
 */
public class CacheWrapper<T> implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 缓存数据
     */
    private T cacheObject;

    /**
     * 最后加载时间
     */
    private long lastLoadTime;

    public CacheWrapper() {
    }

    public CacheWrapper(T cacheObject) {
        this.cacheObject=cacheObject;
        this.lastLoadTime=System.currentTimeMillis();
    }

    public long getLastLoadTime() {
        return lastLoadTime;
    }

    public void setLastLoadTime(long lastLoadTime) {
        this.lastLoadTime=lastLoadTime;
    }

    public T getCacheObject() {
        return cacheObject;
    }

    public void setCacheObject(T cacheObject) {
        this.cacheObject=cacheObject;
    }

}
