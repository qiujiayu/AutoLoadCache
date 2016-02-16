package com.jarvis.cache.mybatis;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;

/**
 * 如果要在Mybatis 的mapper上使用@Cache 及@CacheDelete 注解时，需要使用些类获取注解实例 弊端：mapper中的所有方法都会进入此切面 &lt;aop:config proxy-target-class="false"&gt;
 * 配置中 proxy-target-class 必须设置为false
 * @author jiayu.qiu
 */
public class CachePointCutProxy {

    private AbstractCacheManager cacheManager;

    public AbstractCacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(AbstractCacheManager cacheManager) {
        this.cacheManager=cacheManager;
    }

    public Object proceed(ProceedingJoinPoint pjp) throws Exception {
        Signature signature=pjp.getSignature();
        MethodSignature methodSignature=(MethodSignature)signature;
        Method method=methodSignature.getMethod();
        if(method.isAnnotationPresent(Cache.class)) {
            Cache cache=method.getAnnotation(Cache.class);// method.getAnnotationsByType(Cache.class)[0];
            return cacheManager.proceed(pjp, cache);
        }

        try {
            return pjp.proceed();
        } catch(Throwable e) {
            throw new Exception(e);
        }
    }

    public void deleteCache(JoinPoint jp, Object retVal) {
        Signature signature=jp.getSignature();
        MethodSignature methodSignature=(MethodSignature)signature;
        Method method=methodSignature.getMethod();
        if(method.isAnnotationPresent(CacheDelete.class)) {
            CacheDelete cacheDelete=method.getAnnotation(CacheDelete.class);
            cacheManager.deleteCache(jp, cacheDelete, retVal);
        }
    }
}
