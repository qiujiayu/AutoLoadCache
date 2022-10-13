package com.jarvis.cache.to;

import lombok.Data;

/**
 *
 */
@Data
public class ProcessingTO {

    private volatile long startTime;

    private volatile CacheWrapper<Object> cache;

    private volatile boolean firstFinished = false;

    private volatile Throwable error;

    public ProcessingTO() {
        startTime = System.currentTimeMillis();
    }
    
    public CacheWrapper<Object> getCache() {
        return cache;
    }

    public void setCache(CacheWrapper<Object> cache) {
        this.cache = cache;
    }

    public void setFirstFinished(boolean firstFinished) {
        this.firstFinished = firstFinished;
    }

    public boolean isFirstFinished() {
        return this.firstFinished;
    }
}
