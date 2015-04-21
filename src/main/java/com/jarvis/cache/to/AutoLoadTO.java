package com.jarvis.cache.to;

import java.io.Serializable;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 用于处理自动加载数据到缓存
 * @author jiayu.qiu
 */
public class AutoLoadTO implements Serializable {

    private static final long serialVersionUID=1L;

    private ProceedingJoinPoint joinPoint;

    private Object args[];

    /**
     * 缓存Key
     */
    private String cacheKey;

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

    /**
     * 缓存过期时间
     */
    private int expire;

    private long requestTimeout=7200L;// 缓存数据在 requestTimeout 秒之内没有使用了，就不进行自动加载数据

    private boolean loading=false;

    /**
     * 加载次数
     */
    private long loadCnt=0L;

    /**
     * 从DAO中加载数据，使用时间的总和
     */
    private long useTotalTime=0L;

    public AutoLoadTO(String cacheKey, ProceedingJoinPoint joinPoint, Object args[], int expire, long requestTimeout) {
        this.cacheKey=cacheKey;
        this.joinPoint=joinPoint;
        this.args=args;
        this.expire=expire;
        this.requestTimeout=requestTimeout;
    }

    public ProceedingJoinPoint getJoinPoint() {
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

    public int getExpire() {
        return expire;
    }

    public long getLastLoadTime() {
        return lastLoadTime;
    }

    public void setLastLoadTime(long lastLoadTime) {
        this.lastLoadTime=lastLoadTime;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public long getRequestTimeout() {
        return requestTimeout;
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
     * @param useTime
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
}