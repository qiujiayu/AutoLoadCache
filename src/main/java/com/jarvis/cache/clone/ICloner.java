package com.jarvis.cache.clone;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 深度复制
 * 
 * @author jiayu.qiu
 */
public interface ICloner {

    /**
     * 深度复制Object
     * 
     * @param obj Object
     * @param type obj的类型，方便以json来处理时，提升性能,如果获取不到type，则可以为null
     * @return Object
     * @throws Exception 异常
     */
    Object deepClone(Object obj, final Type type) throws Exception;

    /**
     * 深度复制 Method 中的参数
     * 
     * @param method Method
     * @param args 参数
     * @return 参数
     * @throws Exception 异常
     */
    Object[] deepCloneMethodArgs(Method method, Object[] args) throws Exception;
}
