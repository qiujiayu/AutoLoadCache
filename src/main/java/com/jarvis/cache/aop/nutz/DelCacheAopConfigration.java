package com.jarvis.cache.aop.nutz;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.nutz.aop.MethodInterceptor;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.aop.SimpleAopMaker;
import org.nutz.ioc.loader.annotation.IocBean;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.CacheDelete;

@IocBean(name = "$aop_delcache")
public class DelCacheAopConfigration extends SimpleAopMaker<CacheDelete> {

	public List<? extends MethodInterceptor> makeIt(CacheDelete cache, Method method, Ioc ioc) {
		return Arrays.asList(new DelCacheAopInterceptor(ioc.get(AbstractCacheManager.class, "cachePointCut"), cache, method));
	}
}
