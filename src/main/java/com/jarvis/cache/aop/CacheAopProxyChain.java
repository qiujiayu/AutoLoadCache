package com.jarvis.cache.aop;

import java.lang.reflect.Method;

public interface CacheAopProxyChain {

    Object[] getArgs();

    @SuppressWarnings("rawtypes")
    Class getTargetClass();

    Method getMethod();

    Object doProxyChain(Object[] arguments) throws Throwable;
}
