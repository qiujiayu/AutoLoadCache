package com.jarvis.cache.serializer;

import java.lang.reflect.Type;

public interface ISerializer<T> {

    /**
     * Serialize the given object to binary data.
     * @param obj object to serialize
     * @return the equivalent binary data
     * @throws Exception 异常
     */
    byte[] serialize(final T obj) throws Exception;

    /**
     * Deserialize an object from the given binary data.
     * @param bytes object binary representation
     * @param returnType the GenericReturnType of AOP Method
     * @return the equivalent object instance
     * @throws Exception 异常
     */
    T deserialize(final byte[] bytes, final Type returnType) throws Exception;

    /**
     * 深度复制
     * @param obj object to clone
     * @return the equivalent object instance
     * @throws Exception 异常
     */
    T deepClone(T obj) throws Exception;
}
