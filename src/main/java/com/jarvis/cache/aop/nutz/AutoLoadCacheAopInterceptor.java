package com.jarvis.cache.aop.nutz;

import java.lang.reflect.Method;

import org.nutz.aop.InterceptorChain;
import org.nutz.aop.MethodInterceptor;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;

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
				Object obj = cachePointCut.proceed(new CacheAopProxyChain() {

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

					@Override
					public Object doProxyChain(Object[] arguments) throws Throwable {
						chain.doChain();
						return chain.getReturn();
					}
				}, cache);
				chain.setReturnValue(obj);
			} else {
				chain.doChain();
			}
		} catch (Throwable e) {
			throw e;
		}
	}
}
