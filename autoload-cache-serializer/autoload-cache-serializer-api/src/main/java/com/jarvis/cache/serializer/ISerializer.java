package com.jarvis.cache.serializer;

import com.jarvis.cache.clone.ICloner;

import java.lang.reflect.Type;

/**
 *
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
     * @param bytes      object binary representation
     * @param returnType the GenericReturnType of AOP Method
     * @return the equivalent object instance, 必须是CacheWrapper类型的
     * @throws Exception 异常
     */
    T deserialize(final byte[] bytes, final Type returnType) throws Exception;

}
