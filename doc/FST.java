package com.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.nustaq.serialization.FSTConfiguration;

import com.test.hessian.MyTO;

/**
 * 
 * TODO
 * @author: jiayu.qiu
 */
public class FST {

    private static final FSTConfiguration conf=FSTConfiguration.getDefaultConfiguration();
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
        byte barray[]=conf.asByteArray(obj);
        return barray;
    }

    private static Object read(byte[] data) throws Exception {
        return conf.asObject(data);
    }

}
