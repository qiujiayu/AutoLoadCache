package com.jarvis.cache.admin.servlet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.nutz.ioc.Ioc;
import org.nutz.lang.Lang;
import org.nutz.mvc.Mvcs;
import org.nutz.resource.Scans;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.admin.servlet.CacheManagerConfig;

public class NutzCacheManagerConfig implements CacheManagerConfig {

	private Ioc ioc;

	private String[] cacheManagerNames;

	@Override
	public String[] getCacheManagerNames(HttpServletRequest req) {
		return getCacheManagerNames();
	}

	private String[] getCacheManagerNames() {
		if (cacheManagerNames == null) {
			List<Class<?>> list = Scans.me().scanPackage("com.jarvis.cache");
			Set<String> names = new HashSet<String>();
			for (Class<?> clazz : list) {
				if (AbstractCacheManager.class.isAssignableFrom(clazz) && !Lang.equals(clazz.getSimpleName(), AbstractCacheManager.class.getSimpleName())) {
					names.add(clazz.getSimpleName());
				}
			}
			String[] array = new String[names.size()];
			return names.toArray(array);
		}
		return cacheManagerNames;
	}

	@Override
	public AbstractCacheManager getCacheManagerByName(HttpServletRequest req, String cacheManagerName) {
		AbstractCacheManager cacheManager = getIoc().get(AbstractCacheManager.class, "cachePointCut");
		return cacheManager;
	}

	private Ioc getIoc() {
		if (this.ioc == null) {
			this.ioc = Mvcs.getIoc();
		}
		return this.ioc;
	}
}
