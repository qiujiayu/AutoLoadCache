package com.jarvis.cache.aop.aspectj;

import com.jarvis.cache.aop.DeleteCacheTransactionalAopProxyChain;
import org.aspectj.lang.ProceedingJoinPoint;

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
