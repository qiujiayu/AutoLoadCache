package com.test;

import java.lang.reflect.ParameterizedType;
import java.util.Calendar;

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
            CacheWrapper<Simple> wrapper=new CacheWrapper<Simple>();
            wrapper.setCacheObject(Simple.getSimple());
            Class<CacheWrapper> typeClass=(Class<CacheWrapper>)wrapper.getClass();
            ParameterizedType type=(ParameterizedType)typeClass.getGenericSuperclass();
            Class entityClass=(Class)type.getActualTypeArguments()[0];
            // System.out.println("type==" + type);
            System.out.println("entityClass==" + entityClass);
            // System.out.println("getOwnerType==" + type.getOwnerType());
            // System.out.println("getRawType==" + type.getRawType());
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Factory error!!!\n" + e.getMessage());
        }
    }
}
