package com.test;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jarvis.cache.redis.ShardedCachePointCut;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;

public class BeanTest {
    private static final Map<String, Boolean> processing=new ConcurrentHashMap<String, Boolean>();

    public static void main(String args[]) {
        Boolean isProcessing=processing.putIfAbsent("k1", Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
        System.out.println("isProcessing1=="+isProcessing);
        isProcessing=processing.putIfAbsent("k1", Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
        System.out.println("isProcessing2=="+isProcessing);
        
        AutoLoadConfig config=new AutoLoadConfig();
        Class configClass=config.getClass();
        Field fields[]=configClass.getDeclaredFields();
        for(Field field: fields) {
            field.setAccessible(true);
            System.out.println(field.getType().getName());
        }
        System.out.println(ShardedCachePointCut.class.getSuperclass().getSuperclass().getName());
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
