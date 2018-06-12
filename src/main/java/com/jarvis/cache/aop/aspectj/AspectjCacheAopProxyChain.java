package com.jarvis.cache.aop.aspectj;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import com.jarvis.cache.aop.CacheAopProxyChain;

/**
 * @author: jiayu.qiu
 */
public class AspectjCacheAopProxyChain implements CacheAopProxyChain {

    private final ProceedingJoinPoint jp;

    private Method method;

    public AspectjCacheAopProxyChain(ProceedingJoinPoint jp) {
        this.jp = jp;

    }

    @Override
    public Object[] getArgs() {
        return jp.getArgs();
    }

    @Override
    public Object getTarget() {
        return jp.getTarget();
    }

    @Override
    public Method getMethod() {
        if (null == method) {
            Signature signature = jp.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            this.method = methodSignature.getMethod();
        }
        return method;
    }

    @Override
    public Object doProxyChain(Object[] arguments) throws Throwable {
        return jp.proceed(arguments);
    }

}
