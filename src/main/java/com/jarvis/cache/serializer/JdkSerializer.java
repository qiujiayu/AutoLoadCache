package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;

public class JdkSerializer implements ISerializer<Object> {

    @Override
    public Object deserialize(byte[] bytes, Type returnType) throws Exception {
        if(null == bytes || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream inputStream=new ByteArrayInputStream(bytes);
        ObjectInputStream input=new ObjectInputStream(inputStream);
        return input.readObject();
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if(obj == null) {
            return new byte[0];
        }
        // 将对象写到流里
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        ObjectOutputStream output=new ObjectOutputStream(outputStream);
        output.writeObject(obj);
        output.flush();
        return outputStream.toByteArray();
    }

    @Override
    public Object deepClone(Object obj) throws Exception {
        return deserialize(serialize(obj), null);
    }
}
