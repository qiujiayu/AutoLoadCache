package com.jarvis.cache.aop.nutz;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.nutz.aop.MethodInterceptor;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.aop.SimpleAopMaker;
import org.nutz.ioc.loader.annotation.IocBean;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.Cache;

@IocBean(name = "$aop_autoloadcache")
public class AutoLoadCacheAopConfigration extends SimpleAopMaker<Cache> {

	public List<? extends MethodInterceptor> makeIt(Cache cache, Method method, Ioc ioc) {
		return Arrays.asList(new AutoLoadCacheAopInterceptor(ioc.get(AbstractCacheManager.class, "cachePointCut"), cache, method));
	}
}
