package com.jarvis.cache.interceptor.aopproxy;

import org.aopalliance.intercept.MethodInvocation;

import com.jarvis.cache.aop.DeleteCacheTransactionalAopProxyChain;

public class DeleteCacheTransactionalAopProxy implements DeleteCacheTransactionalAopProxyChain {

    private final MethodInvocation invocation;

    public DeleteCacheTransactionalAopProxy(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public Object doProxyChain() throws Throwable {
        return invocation.proceed();
    }

}
