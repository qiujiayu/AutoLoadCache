package com.jarvis.cache.clone;

import java.lang.reflect.Method;

/**
 * 深度复制
 * @author jiayu.qiu
 */
public interface ICloner {

    /**
     * 深度复制Object
     * @param obj Object
     * @return Object
     * @throws Exception
     */
    Object deepClone(Object obj) throws Exception;

    /**
     * 深度复制 Method 中的参数
     * @param method Method
     * @param args 参数
     * @return 参数
     * @throws Exception
     */
    Object[] deepCloneMethodArgs(Method method, Object[] args) throws Exception;
}
