package com.jarvis.cache.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.jarvis.cache.to.CacheWrapper;

/**
 * autoload-cache CacheWrapper Serializer
 *
 * @author stevie.wong
 */
public class CacheWrapperSerializer extends Serializer<CacheWrapper> {

    @Override
    @SuppressWarnings("unchecked")
    public void write(Kryo kryo, Output output, CacheWrapper object) {
        output.writeInt(object.getExpire(), true);
        output.writeLong(object.getLastLoadTime(), true);
        kryo.writeClassAndObject(output, object.getCacheObject());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CacheWrapper read(Kryo kryo, Input input, Class<CacheWrapper> type) {
        int expire = input.readInt(true);
        long lastLoadTime = input.readLong(true);
        Object o = kryo.readClassAndObject(input);
        CacheWrapper cacheWrapper = new CacheWrapper();
        cacheWrapper.setCacheObject(o);
        cacheWrapper.setExpire(expire);
        cacheWrapper.setLastLoadTime(lastLoadTime);
        return cacheWrapper;
    }

}
