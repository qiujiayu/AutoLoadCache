package com.jarvis.cache.to;

import lombok.Data;

import java.io.Serializable;

/**
 * 对缓存数据进行封装
 *
 *
 */
@Data
public class CacheWrapper<T> implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /**
     * 缓存数据
     */
    private T cacheObject;

    /**
     * 最后加载时间
     */
    private long lastLoadTime;

    /**
     * 缓存时长
     */
    private int expire;

    public CacheWrapper() {
    }

    public CacheWrapper(T cacheObject, int expire) {
        this.cacheObject = cacheObject;
        this.lastLoadTime = System.currentTimeMillis();
        this.expire = expire;
    }

    public CacheWrapper(T cacheObject, int expire, long lastLoadTime) {
        this.cacheObject = cacheObject;
        this.lastLoadTime = lastLoadTime;
        this.expire = expire;
    }

    /**
     * 判断缓存是否已经过期
     *
     * @return boolean
     */
    public boolean isExpired() {
        if (expire > 0) {
            return (System.currentTimeMillis() - lastLoadTime) > expire * 1000;
        }
        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        @SuppressWarnings("unchecked")
        CacheWrapper<T> tmp = (CacheWrapper<T>) super.clone();
        tmp.setCacheObject(this.cacheObject);
        return tmp;
    }

}
