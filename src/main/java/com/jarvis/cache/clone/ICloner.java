package com.jarvis.cache.clone;

/**
 * 深度复制
 * @author jiayu.qiu
 */
public interface ICloner {

    Object deepClone(Object obj) throws Exception;
}
