package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

public class HessianSerializer implements ISerializer<Object> {

    private static final SerializerFactory serializerFactory=new SerializerFactory();
    static {
        serializerFactory.addFactory(new HessionBigDecimalSerializerFactory());
        serializerFactory.addFactory(new HessionSoftReferenceSerializerFactory());
    }

    /**
     * 添加自定义SerializerFactory
     * @param factory AbstractSerializerFactory
     */
    public void addSerializerFactory(AbstractSerializerFactory factory) {
        serializerFactory.addFactory(factory);
    }

    @Override
    public byte[] serialize(final Object obj) throws Exception {
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
    public Object deserialize(final byte[] bytes, final Type returnType) throws Exception {
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

    @Override
    public Object deepClone(Object obj) throws Exception {
        return deserialize(serialize(obj), null);
    }

}
