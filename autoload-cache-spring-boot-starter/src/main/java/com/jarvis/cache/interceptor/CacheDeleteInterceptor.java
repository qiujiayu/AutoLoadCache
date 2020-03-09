package com.jarvis.cache.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

import com.jarvis.cache.CacheHandler;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.autoconfigure.AutoloadCacheProperties;
import com.jarvis.cache.interceptor.aopproxy.DeleteCacheAopProxy;
import com.jarvis.cache.util.AopUtil;

/**
 * 对@CacheDelete 拦截注解
 * 
 *
 */
public class CacheDeleteInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CacheDeleteInterceptor.class);

    private final CacheHandler cacheHandler;

    private final AutoloadCacheProperties config;

    public CacheDeleteInterceptor(CacheHandler cacheHandler, AutoloadCacheProperties config) {
        this.cacheHandler = cacheHandler;
        this.config = config;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!this.config.isEnable()) {
            return invocation.proceed();
        }
        Method method = invocation.getMethod();
        // if (method.getDeclaringClass().isInterface()) {
        Class<?> cls = AopUtil.getTargetClass(invocation.getThis());
        if (!cls.equals(invocation.getThis().getClass())) {
            if (logger.isDebugEnabled()) {
                logger.debug(invocation.getThis().getClass() + "-->" + cls);
            }
            return invocation.proceed();
        }
        // }
        Object result = invocation.proceed();
        if (method.isAnnotationPresent(CacheDelete.class)) {
            CacheDelete cacheDelete = method.getAnnotation(CacheDelete.class);
            if (logger.isDebugEnabled()) {
                logger.debug(invocation.getThis().getClass().getName() + "." + method.getName() + "-->@CacheDelete");
            }
            cacheHandler.deleteCache(new DeleteCacheAopProxy(invocation), cacheDelete, result);
        } else {
            Method specificMethod = AopUtils.getMostSpecificMethod(method, invocation.getThis().getClass());
            if (specificMethod.isAnnotationPresent(CacheDelete.class)) {
                CacheDelete cacheDelete = specificMethod.getAnnotation(CacheDelete.class);
                if (logger.isDebugEnabled()) {
                    logger.debug(invocation.getThis().getClass().getName() + "." + specificMethod.getName()
                            + "-->@CacheDelete");
                }
                cacheHandler.deleteCache(new DeleteCacheAopProxy(invocation), cacheDelete, result);
            }
        }
        return result;
    }

}
