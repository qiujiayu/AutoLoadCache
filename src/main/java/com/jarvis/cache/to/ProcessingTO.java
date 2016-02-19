package com.jarvis.cache.to;

import java.util.concurrent.atomic.AtomicInteger;

public class ProcessingTO {

    private AtomicInteger counter=new AtomicInteger(0);

    private CacheWrapper cache;

    private boolean firstFinished=false;

    public AtomicInteger getCounter() {
        return counter;
    }

    public void setCounter(AtomicInteger counter) {
        this.counter=counter;
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

}
