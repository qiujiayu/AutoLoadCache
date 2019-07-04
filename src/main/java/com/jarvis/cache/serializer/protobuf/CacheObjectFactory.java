package com.jarvis.cache.serializer.protobuf;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @author zhengenshen@gmail.com
 */
public class CacheObjectFactory {

    public static class ReadByteBufPooledFactory extends BasePooledObjectFactory<ReadByteBuf> {
        @Override
        public ReadByteBuf create() {
            return new ReadByteBuf();
        }

        @Override
        public PooledObject<ReadByteBuf> wrap(ReadByteBuf obj) {
            return new DefaultPooledObject<>(obj);
        }
    }

    public static class WriteByteBufPooledFactory extends BasePooledObjectFactory<WriteByteBuf> {

        @Override
        public WriteByteBuf create() {
            return new WriteByteBuf();
        }

        @Override
        public PooledObject<WriteByteBuf> wrap(WriteByteBuf obj) {
            return new DefaultPooledObject<>(obj);
        }
    }


    public static WriteByteBufPooledFactory createWriteByteBuf() {
        return new WriteByteBufPooledFactory();
    }

    public static ReadByteBufPooledFactory createReadByteBuf() {
        return new ReadByteBufPooledFactory();
    }

}
