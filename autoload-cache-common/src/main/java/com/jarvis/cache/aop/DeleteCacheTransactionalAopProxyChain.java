package com.jarvis.cache.aop;

/**
 *
 */
public interface DeleteCacheTransactionalAopProxyChain {

    /**
     * 执行方法
     *
     * @return 执行结果
     * @throws Throwable Throwable
     */
    Object doProxyChain() throws Throwable;
}
