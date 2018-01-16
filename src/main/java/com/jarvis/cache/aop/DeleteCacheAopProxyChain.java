package com.jarvis.cache.aop;

import java.lang.reflect.Method;

/**
 * 
 * @author: jiayu.qiu
 */
public interface DeleteCacheAopProxyChain {

    /**
     * 
     * 获取参数
     * @return
     */
    Object[] getArgs();

    
    /**
     * 获取目标实例
     * @return
     */
    Object getTarget();

    /**
     * 
     * 获取方法
     * @return
     */
    Method getMethod();

}
