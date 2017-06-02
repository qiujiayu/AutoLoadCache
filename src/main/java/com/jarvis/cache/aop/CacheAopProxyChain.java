package com.jarvis.cache.aop;

import java.lang.reflect.Method;

public interface CacheAopProxyChain {

    /**
     * 获取参数
     * @return 参数
     */
    Object[] getArgs();

    @SuppressWarnings("rawtypes")
    Class getTargetClass();

    /**
     * 获取方法
     * @return Method
     */
    Method getMethod();

    /**
     * 执行方法
     * @param arguments 参数
     * @return 执行结果
     * @throws Throwable Throwable
     */
    Object doProxyChain(Object[] arguments) throws Throwable;
}
