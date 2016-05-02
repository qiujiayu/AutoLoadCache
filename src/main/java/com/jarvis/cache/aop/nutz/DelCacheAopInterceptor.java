package com.jarvis.cache.aop.nutz;

import java.lang.reflect.Method;

import org.nutz.aop.InterceptorChain;
import org.nutz.aop.MethodInterceptor;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.aop.DeleteCacheAopProxyChain;

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
				chain.doChain();
				cacheManager.deleteCache(new DeleteCacheAopProxyChain() {

					@Override
					public Class<?> getTargetClass() {
						return chain.getCallingMethod().getDeclaringClass();
					}

					@Override
					public Method getMethod() {
						return chain.getCallingMethod();
					}

					@Override
					public Object[] getArgs() {
						return chain.getArgs();
					}
				}, cache, chain.getReturn());
			} else {
				chain.doChain();
			}
		} catch (Throwable e) {
			throw e;
		}
	}
}
