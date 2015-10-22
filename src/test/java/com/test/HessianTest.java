package com.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.jarvis.cache.serializer.JdkSerializer;
import com.jarvis.cache.to.CacheWrapper;

public class HessianTest {

    public static void main(String[] args) throws Exception {
        write();
        read();
    }

    private static void write() throws Exception {
        HessianOutput output=new HessianOutput(new FileOutputStream("hessian.bin"));

        CacheWrapper<Simple> wrapper=new CacheWrapper<Simple>();
        wrapper.setCacheObject(Simple.getSimple());

        output.writeObject(wrapper);
        output.flush();
        output.close();
        // System.out.println(bo.toByteArray().length);
        System.out.println(new JdkSerializer().serialize(wrapper).length);
    }

    private static void read() throws Exception {
        InputStream inputStream=new FileInputStream("hessian.bin");
        HessianInput input=new HessianInput(inputStream);

        System.out.println(inputStream.available());
        // Simple someObject = kryo.readObject(input, Simple.class);
        @SuppressWarnings("unchecked")
        CacheWrapper<Simple> someObject=(CacheWrapper<Simple>)input.readObject();
        input.close();
        System.out.println(someObject.getCacheObject());
    }
}
