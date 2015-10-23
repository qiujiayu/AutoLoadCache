package com.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.jarvis.cache.serializer.JdkSerializer;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;

public class JdkSerializerTest {

    public static void main(String[] args) throws Exception {
        long start=System.currentTimeMillis();
        CacheWrapper<Simple> wrapper=new CacheWrapper<Simple>();
        wrapper.setCacheObject(Simple.getSimple());
        
        BeanUtil.deepClone(wrapper, new JdkSerializer());
        String fileName="jdk.bin";
        for(int i=0; i < 1000; i++) {
            write(wrapper, new FileOutputStream(fileName));
        }
        long end=System.currentTimeMillis();
        System.out.println("write:" + (end - start));

        start=System.currentTimeMillis();
        for(int i=0; i < 1000; i++) {
            InputStream inputStream=new FileInputStream(fileName);
            if(i == 0) {
                System.out.println("size:" + inputStream.available());
            }
            read(inputStream);
        }
        end=System.currentTimeMillis();
        System.out.println("read:" + (end - start));
    }

    private static void write(Object obj, OutputStream outputStream) throws Exception {
        ObjectOutputStream output=new ObjectOutputStream(new GZIPOutputStream(outputStream));
        output.writeObject(obj);
        output.flush();
        output.close();
        // System.out.println(bo.toByteArray().length);
        // System.out.println(new JdkSerializer().serialize(wrapper).length);
    }

    private static void read(InputStream inputStream) throws Exception {
        ObjectInputStream input=new ObjectInputStream(new GZIPInputStream(inputStream));
        @SuppressWarnings("unchecked")
        CacheWrapper<Simple> someObject=(CacheWrapper<Simple>)input.readObject();
        input.close();
        //System.out.println(someObject.getCacheObject());
    }
}
