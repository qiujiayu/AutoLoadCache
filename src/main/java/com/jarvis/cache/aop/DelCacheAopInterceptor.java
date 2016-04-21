package com.jarvis.cache.aop;

import java.lang.reflect.Method;

import org.nutz.aop.InterceptorChain;
import org.nutz.aop.MethodInterceptor;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.CacheDelete;

public class DelCacheAopInterceptor implements MethodInterceptor {

	private AbstractCacheManager cacheManager;

	private CacheDelete cache;

	private boolean haveCache;

	public DelCacheAopInterceptor(AbstractCacheManager cacheManager, CacheDelete cache, Method method) {
		this.cacheManager = cacheManager;
		this.cache = cache;
		if (method.isAnnotationPresent(CacheDelete.class)) {
			this.haveCache = true;
		}
	}

	public void filter(final InterceptorChain chain) throws Throwable {
		try {
			if (haveCache) {
				cacheManager.deleteCache(chain, cache, chain.getArgs());
			} else {
				chain.doChain();
			}
		} catch (Throwable e) {
			throw e;
		}
	}
}
