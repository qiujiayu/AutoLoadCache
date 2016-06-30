package com.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.jarvis.cache.serializer.JdkSerializer;
import com.jarvis.cache.to.CacheWrapper;

public class JdkSerializerTest {

    public static void main(String[] args) throws Exception {
        long start=System.currentTimeMillis();
        CacheWrapper wrapper=new CacheWrapper();
        wrapper.setCacheObject(Simple.getSimple());

        new JdkSerializer().deepClone(wrapper, null);
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
        ObjectOutputStream output=new ObjectOutputStream(outputStream);
        output.writeObject(obj);
        output.flush();
        return outputStream.toByteArray();
        // System.out.println(bo.toByteArray().length);
        // System.out.println(new JdkSerializer().serialize(wrapper).length);
    }

    private static void read(byte[] data) throws Exception {
        ByteArrayInputStream inputStream=new ByteArrayInputStream(data);
        ObjectInputStream input=new ObjectInputStream(inputStream);
        CacheWrapper someObject=(CacheWrapper)input.readObject();
        input.close();
        // System.out.println(someObject.getCacheObject());
    }
}
