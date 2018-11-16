package com.jarvis.cache.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

/**
 * kryo class register
 *
 * @author stevie.wong
 */
public interface KryoClassRegistration {

    /**
     * 注册类
     * @param kryo see {@link Kryo}
     */
    void register(Kryo kryo);
}
