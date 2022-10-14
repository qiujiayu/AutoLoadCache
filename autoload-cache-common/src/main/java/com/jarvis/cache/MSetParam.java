package com.jarvis.cache;

import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 *
 */
public class MSetParam {

    private CacheKeyTO cacheKey;

    private CacheWrapper<Object> result;

    public MSetParam(CacheKeyTO cacheKey, CacheWrapper<Object> result) {
        this.cacheKey = cacheKey;
        this.result = result;
    }
    
    public CacheKeyTO getCacheKey() {
        return cacheKey;
    }

    public CacheWrapper<Object> getResult() {
        return result;
    }
}
