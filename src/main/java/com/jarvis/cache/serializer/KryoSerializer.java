package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoSerializer implements ISerializer<Object> {

    private static Kryo kryo=new Kryo();
    static {
        kryo.setReferences(false);
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if(obj == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        Output output=new Output(outputStream);

        // 将对象写到流里
        kryo.writeClassAndObject(output, obj);
        output.flush();
        byte[] val=outputStream.toByteArray();
        output.close();
        return val;
    }

    @Override
    public Object deserialize(byte[] bytes) throws Exception {
        if(null == bytes || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream inputStream=new ByteArrayInputStream(bytes);
        Input input=new Input(inputStream);

        Object obj=kryo.readClassAndObject(input);
        input.close();
        return obj;
    }

}
