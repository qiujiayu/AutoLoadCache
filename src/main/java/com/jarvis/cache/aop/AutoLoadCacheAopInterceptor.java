package com.jarvis.cache.aop;

import java.lang.reflect.Method;

import org.nutz.aop.InterceptorChain;
import org.nutz.aop.MethodInterceptor;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.Cache;

public class AutoLoadCacheAopInterceptor implements MethodInterceptor {

	private final static Log log = Logs.get();

	private AbstractCacheManager cachePointCut;

	private Cache cache;

	private boolean haveCache;

	public AutoLoadCacheAopInterceptor(AbstractCacheManager cacheManager, Cache cache, Method method) {
		this.cachePointCut = cacheManager;
		this.cache = cache;
		if (method.isAnnotationPresent(Cache.class)) {
			if (log.isDebugEnabled()) {
				log.debugf("class %s , method %s", method.getDeclaringClass().getName(), method.getName());
			}
			this.haveCache = true;
		}
	}

	public void filter(final InterceptorChain chain) throws Throwable {
		try {
			if (haveCache) {
				Object obj = cachePointCut.proceed(chain, cache);
				chain.setReturnValue(obj);
			} else {
				chain.doChain();
			}
		} catch (Throwable e) {
			throw e;
		}
	}
}
