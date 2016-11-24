package com.jarvis.cache.serializer;

import org.nustaq.serialization.FSTConfiguration;

/**
 * 使用 https://github.com/RuedigerMoeller/fast-serialization 进行序列化和反序列化
 * @author jiayu.qiu
 */
public class FSTSerializer implements ISerializer<Object> {

    private static final FSTConfiguration conf=FSTConfiguration.getDefaultConfiguration();

    @Override
    public byte[] serialize(Object obj) throws Exception {
        byte barray[]=conf.asByteArray(obj);
        return barray;
    }

    @Override
    public Object deserialize(byte[] data) throws Exception {
        return conf.asObject(data);
    }

}
