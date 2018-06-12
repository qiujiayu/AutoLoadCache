package com.jarvis.cache.serializer;

import java.lang.reflect.Type;

import com.jarvis.cache.clone.ICloner;

/**
 * @author: jiayu.qiu
 */
public interface ISerializer<T> extends ICloner {

    /**
     * Serialize the given object to binary data.
     * 
     * @param obj object to serialize
     * @return the equivalent binary data
     * @throws Exception 异常
     */
    byte[] serialize(final T obj) throws Exception;

    /**
     * Deserialize an object from the given binary data.
     * 
     * @param bytes object binary representation
     * @param returnType the GenericReturnType of AOP Method
     * @return the equivalent object instance
     * @throws Exception 异常
     */
    T deserialize(final byte[] bytes, final Type returnType) throws Exception;

}
