package com.jarvis.cache.aop;

import java.lang.reflect.Method;

/**
 * @author: jiayu.qiu
 */
public interface DeleteCacheAopProxyChain {

    /**
     * 获取参数
     * 
     * @return 参数
     */
    Object[] getArgs();

    /**
     * 获取目标实例
     * 
     * @return 目标实例
     */
    Object getTarget();

    /**
     * 获取方法
     * 
     * @return 方法
     */
    Method getMethod();

}
