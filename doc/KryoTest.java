package com.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.test.hessian.MyTO;

/**
 * 
 * TODO
 * @author: jiayu.qiu
 */
public class KryoTest {

    private final static Kryo kryo = new Kryo();
    static {
        // conf.setCrossPlatform(false);

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

        Simple simple=new Simple();
        simple.setAge(10);
        simple.setName("test");
        simple.setSex(1);
        SimpleWrapper wrapper=new SimpleWrapper();
        wrapper.setCacheObject(simple);
        wrapper.setLastLoadTime(System.currentTimeMillis());
        data=write(wrapper);
        SimpleWrapper t=(SimpleWrapper)read(data);
        System.out.println(t.getCacheObject().getName());
    }

    private static byte[] write(Object obj) throws Exception {
        Output output = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output = new Output(baos);
           
            kryo.writeClassAndObject(output, obj);
            output.flush();
            return baos.toByteArray();
        }finally{
            if(output != null) {
                output.close();
            }
        }
    }

    private static Object read(byte[] data) throws Exception {
        if(data == null || data.length == 0) {
            return null;
        }
        Input ois = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ois = new Input(bais);
            return kryo.readClassAndObject(ois);
        } finally {
            if(ois != null) {
                ois.close();
            }
        }
    }

}
