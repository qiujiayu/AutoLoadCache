package com.test.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.jarvis.cache.serializer.HessionBigDecimalSerializerFactory;

public class HessianTest {

    private static SerializerFactory _serializerFactory=SerializerFactory.createDefault();

    static {
        _serializerFactory.addFactory(new HessionBigDecimalSerializerFactory());
    }

    public static void main(String[] args) throws Exception {
        long start=System.currentTimeMillis();
        MyTO to=new MyTO();
        to.setId("111");
        List<String> list=new ArrayList<String>();
        list.add("111");
        list.add("222");
        to.setList(list);

        byte[] data=null;
        for(int i=0; i < 1000; i++) {
            data=write(to);
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

        BigDecimal amount=new BigDecimal(14.0);
        data=write(amount);
        System.out.println(read(data));
    }

    private static byte[] write(Object obj) throws Exception {
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        Hessian2Output output=new Hessian2Output(outputStream);
        output.setSerializerFactory(_serializerFactory);
        output.writeObject(obj);
        output.flush();
        return outputStream.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        ByteArrayInputStream inputStream=new ByteArrayInputStream(data);
        Hessian2Input input=new Hessian2Input(inputStream);
        input.setSerializerFactory(_serializerFactory);
        // Simple someObject = kryo.readObject(input, Simple.class);
        Object obj=input.readObject();
        input.close();
        // System.out.println(someObject.getCacheObject());
        return obj;
    }
}
