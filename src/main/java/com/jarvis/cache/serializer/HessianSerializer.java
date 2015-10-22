package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

public class HessianSerializer implements ISerializer<Object> {

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if(obj == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        HessianOutput output=new HessianOutput(outputStream);

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
        HessianInput input=new HessianInput(inputStream);

        Object obj=input.readObject();
        input.close();
        return obj;
    }

}
