package com.jarvis.cache.aop;

import java.lang.reflect.Method;

public interface CacheAopProxyChain {

    /**
     * @return
     */
    Object[] getArgs();

    @SuppressWarnings("rawtypes")
    Class getTargetClass();

    /**
     * @return
     */
    Method getMethod();

    /**
     * @param arguments
     * @return
     * @throws Throwable
     */
    Object doProxyChain(Object[] arguments) throws Throwable;
}
