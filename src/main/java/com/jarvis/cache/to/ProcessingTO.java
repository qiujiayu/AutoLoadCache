package com.jarvis.cache.to;

public class ProcessingTO {

    private long startTime;

    private CacheWrapper cache;

    private boolean firstFinished=false;

    private Throwable error;

    public ProcessingTO() {
        startTime=System.currentTimeMillis();
    }

    public CacheWrapper getCache() {
        return cache;
    }

    public void setCache(CacheWrapper cache) {
        this.cache=cache;
    }

    public boolean isFirstFinished() {
        return firstFinished;
    }

    public void setFirstFinished(boolean firstFinished) {
        this.firstFinished=firstFinished;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime=startTime;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error=error;
    }

}
