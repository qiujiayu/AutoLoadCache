package com.jarvis.cache;

import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.Data;

/**
 * @author jiayu.qiu
 */
@Data
public class MSetParam {

    private final CacheKeyTO cacheKey;

    private final CacheWrapper<Object> result;

    public MSetParam(CacheKeyTO cacheKey, CacheWrapper<Object> result){
        this.cacheKey = cacheKey;
        this.result = result;
    }
}
