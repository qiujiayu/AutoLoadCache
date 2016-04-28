package com.jarvis.cache.aop;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;

/**
 * 用于处理AOP 拦截
 * @author jiayu.qiu
 *
 * @param <T>
 * @param <M>
 */
public interface AopInterceptor<T, M> {

    public Object proceed(T aopProxyChain, Cache cache) throws Throwable;

    public void deleteCache(M aopProxyChain, CacheDelete cacheDelete, Object retVal);
}
