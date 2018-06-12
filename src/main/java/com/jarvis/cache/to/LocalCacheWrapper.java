package com.jarvis.cache.to;

/**
 * 本地缓存
 * 
 * @author jiayu.qiu
 */
public class LocalCacheWrapper<T> extends CacheWrapper<T> {

    /**
     * 
     */
    private static final long serialVersionUID = -8194389703805635133L;

    /**
     * 远程缓存最后加载时间
     */
    private long remoteLastLoadTime;

    /**
     * 远程缓存的缓存时长
     */
    private int remoteExpire;

    public long getRemoteLastLoadTime() {
        return remoteLastLoadTime;
    }

    public void setRemoteLastLoadTime(long remoteLastLoadTime) {
        this.remoteLastLoadTime = remoteLastLoadTime;
    }

    public int getRemoteExpire() {
        return remoteExpire;
    }

    public void setRemoteExpire(int remoteExpire) {
        this.remoteExpire = remoteExpire;
    }

}
