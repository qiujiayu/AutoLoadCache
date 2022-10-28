package com.jarvis.cache.admin;


public class AutoLoadVO {

    private String namespace;

    private String key;

    private String hfield;

    private String method;

    private String firstRequestTime;

    private String lastRequestTime;

    private long requestTimes;

    private long expire;

    private String expireTime;

    private long requestTimeout;

    private String requestTimeoutTime;

    private String lastLoadTime;

    private long loadCount;

    private long averageUseTime;
    
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHfield() {
        return hfield;
    }

    public void setHfield(String hfield) {
        this.hfield = hfield;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getFirstRequestTime() {
        return firstRequestTime;
    }

    public void setFirstRequestTime(String firstRequestTime) {
        this.firstRequestTime = firstRequestTime;
    }

    public String getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(String lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    public long getRequestTimes() {
        return requestTimes;
    }

    public void setRequestTimes(long requestTimes) {
        this.requestTimes = requestTimes;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getRequestTimeoutTime() {
        return requestTimeoutTime;
    }

    public void setRequestTimeoutTime(String requestTimeoutTime) {
        this.requestTimeoutTime = requestTimeoutTime;
    }

    public String getLastLoadTime() {
        return lastLoadTime;
    }

    public void setLastLoadTime(String lastLoadTime) {
        this.lastLoadTime = lastLoadTime;
    }

    public long getLoadCount() {
        return loadCount;
    }

    public void setLoadCount(long loadCount) {
        this.loadCount = loadCount;
    }

    public long getAverageUseTime() {
        return averageUseTime;
    }

    public void setAverageUseTime(long averageUseTime) {
        this.averageUseTime = averageUseTime;
    }
}
