package com.test.fastjson;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.jarvis.cache.reflect.generics.ParameterizedTypeImpl;
import com.jarvis.cache.to.CacheWrapper;

public class FastjsonTest {

    public static void main(String[] args) throws SecurityException, NoSuchMethodException {
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        String json=JSON.toJSONString(user);
        User user2=getData(json, User.class);
        System.out.println(user2);

        List<User> list=new ArrayList<User>();
        list.add(user);
        String listJson=JSON.toJSONString(list);

        Method methods[]=FastjsonTest.class.getDeclaredMethods();
        Type testType=null;
        String testName=null;
        for(Method method: methods) {
            if(method.getName().equals("getData")) {
                try {
                    for(Class cls: method.getParameterTypes()) {
                        System.out.println(cls.getName());
                    }
                    Type returnType=method.getGenericReturnType();
                    User user3=JSON.parseObject(json, returnType);
                    System.out.println(user3);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else if("getUserList".equals(method.getName())) {
                try {
                    Type returnType=method.getGenericReturnType();
                    testType=returnType;
                    testName="getUserList";
                    System.out.println("tt=" + returnType.getClass().getName());
                    List<User> list2=JSON.parseObject(listJson, returnType);
                    System.out.println(list2);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else if("getUser".equals(method.getName())) {
                Type returnType=method.getGenericReturnType();
                // testType=returnType;
                // testName="getUser";
            }
        }
        List<User> list2=(List<User>)deepClone(list);
        System.out.println("list2=" + list2);
        Type[] agsType=new Type[]{testType};

        ParameterizedTypeImpl ss=ParameterizedTypeImpl.make(CacheWrapper.class, agsType, null);
        System.out.println(ss);
        if(testType instanceof Class<?>) {
            CacheWrapper<User> cache=new CacheWrapper<User>();
            cache.setCacheObject(user2);
            json=JSON.toJSONString(cache);
            cache=JSON.parseObject(json, ss);
            System.out.println(cache.getCacheObject());
        } else if(testType instanceof ParameterizedType) {
            CacheWrapper<List<User>> cache=new CacheWrapper<List<User>>();
            cache.setCacheObject(list2);
            json=JSON.toJSONString(cache);
            cache=JSON.parseObject(json, ss);
            System.out.println(cache.getCacheObject());
        }
    }

    public static <T> T getData(String json, Class<T> type) {
        return (T)JSON.parseObject(json, type);
    }

    public static List<User> getUserList(String json, Type type) {
        return JSON.parseObject(json, type);
    }

    public static User getUser() {
        return null;
    }

    public static Object deepClone(Object obj) {
        Class<? extends Object> clazz=obj.getClass();
        String json=JSON.toJSONString(obj);
        return JSON.parseObject(json, clazz);
    }

}
