package com.jarvis.cache.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

import com.jarvis.cache.CacheHandler;
import com.jarvis.cache.annotation.CacheDeleteTransactional;
import com.jarvis.cache.autoconfigure.AutoloadCacheProperties;
import com.jarvis.cache.interceptor.aopproxy.DeleteCacheTransactionalAopProxy;
import com.jarvis.cache.util.AopUtil;

/**
 * 对@CacheDeleteTransactional 拦截注解
 * 
 * @author jiayu.qiu
 */
public class CacheDeleteTransactionalInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CacheDeleteTransactionalInterceptor.class);

    private final CacheHandler cacheHandler;

    private final AutoloadCacheProperties config;

    public CacheDeleteTransactionalInterceptor(CacheHandler cacheHandler, AutoloadCacheProperties config) {
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
        if (logger.isDebugEnabled()) {
            logger.debug(invocation.toString());
        }
        if (method.isAnnotationPresent(CacheDeleteTransactional.class)) {
            CacheDeleteTransactional cacheDeleteTransactional = method.getAnnotation(CacheDeleteTransactional.class);
            if (logger.isDebugEnabled()) {
                logger.debug(invocation.getThis().getClass().getName() + "." + method.getName()
                        + "-->@CacheDeleteTransactional");
            }
            return cacheHandler.proceedDeleteCacheTransactional(new DeleteCacheTransactionalAopProxy(invocation),
                    cacheDeleteTransactional);
        } else {
            Method specificMethod = AopUtils.getMostSpecificMethod(method, invocation.getThis().getClass());
            if (specificMethod.isAnnotationPresent(CacheDeleteTransactional.class)) {
                CacheDeleteTransactional cacheDeleteTransactional = specificMethod
                        .getAnnotation(CacheDeleteTransactional.class);
                if (logger.isDebugEnabled()) {
                    logger.debug(invocation.getThis().getClass().getName() + "." + specificMethod.getName()
                            + "-->@CacheDeleteTransactional");
                }
                return cacheHandler.proceedDeleteCacheTransactional(new DeleteCacheTransactionalAopProxy(invocation),
                        cacheDeleteTransactional);
            }
        }
        return invocation.proceed();
    }

}
