package com.jarvis.cache.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;

import com.jarvis.cache.aop.DeleteCacheTransactionalAopProxyChain;

/**
 * @author: jiayu.qiu
 */
public class AspectjDeleteCacheTransactionalAopProxyChain implements DeleteCacheTransactionalAopProxyChain {

    private final ProceedingJoinPoint jp;

    public AspectjDeleteCacheTransactionalAopProxyChain(ProceedingJoinPoint jp) {
        this.jp = jp;

    }

    @Override
    public Object doProxyChain() throws Throwable {
        return jp.proceed();
    }

}
