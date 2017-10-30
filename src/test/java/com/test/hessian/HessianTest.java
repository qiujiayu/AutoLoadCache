package com.test.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.jarvis.cache.serializer.hession.HessionBigDecimalSerializerFactory;

/**
 * @author: jiayu.qiu
 */
public class HessianTest {

    private static SerializerFactory SERIALIZER_FACTORY=SerializerFactory.createDefault();

    static {
        // BigDecimal序列化
        SERIALIZER_FACTORY.addFactory(new HessionBigDecimalSerializerFactory());
    }

    public static void main(String[] args) throws Exception {
        byte[] data=null;
        BigInteger  today=new BigInteger("111111");
        data=write(today);
        System.out.println("today="+read(data));
        long start=System.currentTimeMillis();
        MyTO to=new MyTO();
        to.setId("111");
        List<String> list=new ArrayList<String>();
        list.add("111");
        list.add("222");
        to.setList(list);

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
        output.setSerializerFactory(SERIALIZER_FACTORY);
        output.writeObject(obj);
        output.flush();
        return outputStream.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        ByteArrayInputStream inputStream=new ByteArrayInputStream(data);
        Hessian2Input input=new Hessian2Input(inputStream);
        input.setSerializerFactory(SERIALIZER_FACTORY);
        // Simple someObject = kryo.readObject(input, Simple.class);
        Object obj=input.readObject();
        input.close();
        // System.out.println(someObject.getCacheObject());
        return obj;
    }
}
