package com.jarvis.cache.to;

import java.util.Objects;

/**
 *
 */
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
    
    public long getStartTime() {
        return startTime;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return this.error;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingTO that = (ProcessingTO) o;
        return startTime == that.startTime &&
                firstFinished == that.firstFinished &&
                Objects.equals(cache, that.cache) &&
                Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, cache, firstFinished, error);
    }

    @Override
    public String toString() {
        return "ProcessingTO{" +
                "startTime=" + startTime +
                ", cache=" + cache +
                ", firstFinished=" + firstFinished +
                ", error=" + error +
                '}';
    }
}
