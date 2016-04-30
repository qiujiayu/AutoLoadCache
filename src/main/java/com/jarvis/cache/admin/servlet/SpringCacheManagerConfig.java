package com.jarvis.cache.admin.servlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.jarvis.cache.AbstractCacheManager;

public class SpringCacheManagerConfig implements CacheManagerConfig {

    private ApplicationContext ctx;

    @Override
    public String[] getCacheManagerNames(HttpServletRequest req) {
        getApplicationContext(req);
        if(null == ctx) {
            return null;
        }
        String cacheManagerNames[]=ctx.getBeanNamesForType(AbstractCacheManager.class);
        return cacheManagerNames;
    }

    @Override
    public AbstractCacheManager getCacheManagerByName(HttpServletRequest req, String cacheManagerName) {
        getApplicationContext(req);
        if(null == ctx) {
            return null;
        }
        AbstractCacheManager cacheManager=(AbstractCacheManager)ctx.getBean(cacheManagerName);
        return cacheManager;
    }

    private void getApplicationContext(HttpServletRequest req) {
        if(null == ctx) {
            ctx=WebApplicationContextUtils.getRequiredWebApplicationContext(req.getSession().getServletContext());
        }
    }

}
