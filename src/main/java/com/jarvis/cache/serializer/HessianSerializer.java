package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

public class HessianSerializer implements ISerializer<Object> {
    
    private static final SerializerFactory serializerFactory = new SerializerFactory();

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if(obj == null) {
            return null;
        }

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        AbstractHessianOutput output=new Hessian2Output(outputStream);
        output.setSerializerFactory(serializerFactory);
        // 将对象写到流里
        output.writeObject(obj);
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
        AbstractHessianInput input=new Hessian2Input(inputStream);
        input.setSerializerFactory(serializerFactory);
        Object obj=input.readObject();
        input.close();
        return obj;
    }

}
