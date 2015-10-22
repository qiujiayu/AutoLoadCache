package com.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.jarvis.cache.serializer.JdkSerializer;
import com.jarvis.cache.to.CacheWrapper;

public class KryoTest {

    private static Kryo kryo=new Kryo();
    static {
        kryo.setReferences(false);
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        // kryo.setAutoReset(true);
        // kryo.register(CacheWrapper.class, 10);
        // kryo.register(Simple.class, 11);
    }

    public static void main(String[] args) throws Exception {
        write();
        read();
    }

    private static void write() throws Exception {
        Output output=new Output(new FileOutputStream("file.bin"));
        CacheWrapper<Simple> wrapper=new CacheWrapper<Simple>();
        wrapper.setCacheObject(Simple.getSimple());

        kryo.writeClassAndObject(output, wrapper);
        output.flush();
        // System.out.println(bo.toByteArray().length);
        System.out.println(new JdkSerializer().serialize(wrapper).length);
    }

    private static void read() throws Exception {
        Input input=new Input(new FileInputStream("file.bin"));

        System.out.println(input.available());
        // Simple someObject = kryo.readObject(input, Simple.class);
        @SuppressWarnings("unchecked")
        CacheWrapper<Simple> someObject=(CacheWrapper<Simple>)kryo.readClassAndObject(input);
        input.close();
        System.out.println(someObject.getCacheObject());
    }

}
