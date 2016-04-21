package com.jarvis.cache.aop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.nutz.aop.MethodInterceptor;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.aop.SimpleAopMaker;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.Cache;

@IocBean(name = "$aop_autoLoadCache")
public class AutoLoadCacheAopConfigration extends SimpleAopMaker<Cache> {

	@Inject
	private AbstractCacheManager cachePointCut;

	public List<? extends MethodInterceptor> makeIt(Cache cache, Method method, Ioc ioc) {
		return Arrays.asList(new AutoLoadCacheAopInterceptor(cachePointCut, cache, method));
	}

	public String[] getName() {
		return new String[0];
	}

	public boolean has(String name) {
		return false;
	}
}
