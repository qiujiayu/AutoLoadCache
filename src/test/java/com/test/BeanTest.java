package com.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;

public class BeanTest {

    public static void main(String args[]) {
        Calendar now=Calendar.getInstance();
        String str=BeanUtil.toString(now.getTime().getTime());
        System.out.println(now.toString());
        System.out.println(String.valueOf(now));
        System.out.println(str);

        CacheWrapper<Simple> wrapper=new CacheWrapper<Simple>();
        wrapper.setCacheObject(Simple.getSimple());
        getService();
    }

    public static void getService() {
        try {
            
            Class entityClass=ReflectUtils.getClassGenricType(SimpleWrapper.class);
            System.out.println("entityClass==" + entityClass);
            // System.out.println("getOwnerType==" + type.getOwnerType());
            // System.out.println("getRawType==" + type.getRawType());
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Factory error!!!\n" + e.getMessage());
        }
    }
}
