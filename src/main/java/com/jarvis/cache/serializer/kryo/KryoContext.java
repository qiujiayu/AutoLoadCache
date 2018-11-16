package com.jarvis.cache.serializer.kryo;

/**
 * kryo接口
 *
 * @author stevie.wong
 */
public interface KryoContext {
    /**
     * 序列化
     * @param obj 对象
     * @return byte[]
     */
    byte[] serialize(Object obj);

    /**
     * 序列化
     * @param obj 对象
     * @param bufferSize 缓冲大小
     * @return byte[]
     */
    byte[] serialize(Object obj, int bufferSize);

    /**
     * 反序列化
     * @param serialized byte[]
     * @return 对象
     */
    Object deserialize(byte[] serialized);
}
