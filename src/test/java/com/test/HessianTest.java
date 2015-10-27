package com.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.jarvis.cache.to.CacheWrapper;

public class HessianTest {

    private static SerializerFactory _serializerFactory=SerializerFactory.createDefault();

    public static void main(String[] args) throws Exception {
        long start=System.currentTimeMillis();
        CacheWrapper<Simple> wrapper=new CacheWrapper<Simple>();
        wrapper.setCacheObject(Simple.getSimple());
        byte[] data=null;
        for(int i=0; i < 1000; i++) {
            data=write(wrapper);
        }
        long end=System.currentTimeMillis();
        System.out.println("write:" + (end - start));
        System.out.println("size:" + data.length);
        start=System.currentTimeMillis();
        for(int i=0; i < 1000; i++) {
            read(data);
        }
        end=System.currentTimeMillis();
        System.out.println("read:" + (end - start));
    }

    private static byte[] write(Object obj) throws Exception {
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        Hessian2Output output=new Hessian2Output(outputStream);
        output.setSerializerFactory(_serializerFactory);
        output.writeObject(obj);
        output.flush();
        return outputStream.toByteArray();
    }

    private static void read(byte[] data) throws Exception {
        ByteArrayInputStream inputStream=new ByteArrayInputStream(data);
        Hessian2Input input=new Hessian2Input(inputStream);
        input.setSerializerFactory(_serializerFactory);
        // Simple someObject = kryo.readObject(input, Simple.class);
        @SuppressWarnings("unchecked")
        CacheWrapper<Simple> someObject=(CacheWrapper<Simple>)input.readObject();
        input.close();
        // System.out.println(someObject.getCacheObject());
    }
}
