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

    private String cacheKey;

    private long lastLoadTime=0;

    private long lastRequestTime=0;

    private int expire;

    private long requestTimeout=7200L;// 缓存数据在 requestTimeout 秒之内没有使用了，就不进行自动加载数据

    private boolean loading=false;

    /**
     * 加载次数
     */
    private long loadCnt=0L;

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
        this.lastRequestTime=lastRequestTime;
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
    public synchronized void addUseTotalTime(long useTime) {
        this.loadCnt++;
        this.useTotalTime+=useTotalTime;
    }

    /**
     * 平均用时
     * @return
     */
    public long getAverageUseTime() {
        if(loadCnt == 0) {
            return 0;
        }
        return this.useTotalTime / this.loadCnt;
    }
}