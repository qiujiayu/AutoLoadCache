package com.jarvis.cache;

import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.Data;

/**
 *
 */
@Data
public class MSetParam {

    private CacheKeyTO cacheKey;

    private CacheWrapper<Object> result;

    public MSetParam(CacheKeyTO cacheKey, CacheWrapper<Object> result) {
        this.cacheKey = cacheKey;
        this.result = result;
    }
}
