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
}
