package com.jarvis.cache.to;

import java.io.Serializable;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;

/**
 * 用于处理自动加载数据到缓存
 * @author jiayu.qiu
 */
public class AutoLoadTO implements Serializable {

    private static final long serialVersionUID=1L;

    private CacheAopProxyChain joinPoint;

    private Object args[];

    /**
     * 缓存注解
     */
    private Cache cache;

    /**
     * 缓存时长
     */
    private int expire;

    /**
     * 缓存Key
     */
    private CacheKeyTO cacheKey;

    /**
     * 上次从DAO加载数据时间
     */
    private long lastLoadTime=0L;

    /**
     * 上次请求数据时间
     */
    private long lastRequestTime=0L;

    /**
     * 第一次请求数据时间
     */
    private long firstRequestTime=0L;

    /**
     * 请求数据次数
     */
    private long requestTimes=0L;

    private volatile boolean loading=false;

    /**
     * 加载次数
     */
    private long loadCnt=0L;

    /**
     * 从DAO中加载数据，使用时间的总和
     */
    private long useTotalTime=0L;

    public AutoLoadTO(CacheKeyTO cacheKey, CacheAopProxyChain joinPoint, Object args[], Cache cache, int expire) {
        this.cacheKey=cacheKey;
        this.joinPoint=joinPoint;
        this.args=args;
        this.cache=cache;
        this.expire=expire;
    }

    public CacheAopProxyChain getJoinPoint() {
        return joinPoint;
    }

    public long getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(long lastRequestTime) {
        synchronized(this) {
            this.lastRequestTime=lastRequestTime;
            if(firstRequestTime == 0) {
                firstRequestTime=lastRequestTime;
            }
            requestTimes++;
        }
    }

    public long getFirstRequestTime() {
        return firstRequestTime;
    }

    public long getRequestTimes() {
        return requestTimes;
    }

    public Cache getCache() {
        return cache;
    }

    public long getLastLoadTime() {
        return lastLoadTime;
    }

    public void setLastLoadTime(long lastLoadTime) {
        if(lastLoadTime > this.lastLoadTime) {
            this.lastLoadTime=lastLoadTime;
        }
    }

    public CacheKeyTO getCacheKey() {
        return cacheKey;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading=loading;
    }

    public Object[] getArgs() {
        return args;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public long getLoadCnt() {
        return loadCnt;
    }

    public long getUseTotalTime() {
        return useTotalTime;
    }

    /**
     * 记录用时
     * @param useTime 用时
     */
    public void addUseTotalTime(long useTime) {
        synchronized(this) {
            this.loadCnt++;
            this.useTotalTime+=useTotalTime;
        }
    }

    /**
     * 平均用时
     * @return long 用时
     */
    public long getAverageUseTime() {
        if(loadCnt == 0) {
            return 0;
        }
        return this.useTotalTime / this.loadCnt;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire=expire;
    }

}