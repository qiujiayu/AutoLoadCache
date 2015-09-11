package com.jarvis.cache.redis;

public interface RedisSerializer<T> {

    /**
     * Serialize the given object to binary data.
     * @param t object to serialize
     * @return the equivalent binary data
     * @throws Exception 异常
     */
    byte[] serialize(T t) throws Exception;

    /**
     * Deserialize an object from the given binary data.
     * @param bytes object binary representation
     * @return the equivalent object instance
     * @throws Exception 异常
     */
    T deserialize(byte[] bytes) throws Exception;
}
