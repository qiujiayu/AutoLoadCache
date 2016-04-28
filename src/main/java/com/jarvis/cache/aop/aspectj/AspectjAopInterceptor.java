package com.jarvis.cache.aop.aspectj;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;

/**
 * 使用Aspectj 实现AOP挂载
 * @author jiayu.qiu
 */
public class AspectjAopInterceptor {

    private AbstractCacheManager cacheManager;

    public Object proceed1(ProceedingJoinPoint pjp) throws Throwable {
        Signature signature=pjp.getSignature();
        MethodSignature methodSignature=(MethodSignature)signature;
        Method method=methodSignature.getMethod();
        if(method.isAnnotationPresent(Cache.class)) {
            Cache cache=method.getAnnotation(Cache.class);// method.getAnnotationsByType(Cache.class)[0];
            return this.proceed(pjp, cache);
        }

        try {
            return pjp.proceed();
        } catch(Throwable e) {
            throw new Exception(e);
        }
    }

    public void deleteCache1(JoinPoint jp, Object retVal) {
        Signature signature=jp.getSignature();
        MethodSignature methodSignature=(MethodSignature)signature;
        Method method=methodSignature.getMethod();
        if(method.isAnnotationPresent(CacheDelete.class)) {
            CacheDelete cacheDelete=method.getAnnotation(CacheDelete.class);
            this.deleteCache(jp, cacheDelete, retVal);
        }
    }

    public Object proceed(ProceedingJoinPoint aopProxyChain, Cache cache) throws Throwable {
        return cacheManager.proceed(new AspectjCacheAopProxyChain(aopProxyChain), cache);
    }

    public void deleteCache(JoinPoint aopProxyChain, CacheDelete cacheDelete, Object retVal) {
        cacheManager.deleteCache(new AspectjDeleteCacheAopProxyChain(aopProxyChain), cacheDelete, retVal);
    }

    public AbstractCacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(AbstractCacheManager cacheManager) {
        this.cacheManager=cacheManager;
    }

}
