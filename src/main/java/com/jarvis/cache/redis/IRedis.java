package com.jarvis.cache.redis;

import java.io.Closeable;

/**
 * Redis缓存操作
 * 
 * @author: jiayu.qiu
 */
public interface IRedis extends Closeable {

    void set(final byte[] key, final byte[] value);

    void setex(final byte[] key, final int seconds, final byte[] value);

    void hset(byte[] key, byte[] field, byte[] value);

    void hset(byte[] key, byte[] field, byte[] value, int seconds);

    byte[] get(byte[] key);

    byte[] hget(final byte[] key, final byte[] field);

    void del(final byte[] key);

    void hdel(final byte[] key, final byte[]... fields);
}
