package com.test.fastjson;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;

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
                    List<User> list2=JSON.parseObject(listJson, returnType);
                    System.out.println(list2);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        List<User> list2=(List<User>)deepClone(list);
        System.out.println("list2=" + list2);
    }

    public static <T> T getData(String json, Class<T> type) {
        return (T)JSON.parseObject(json, type);
    }

    public static List<User> getUserList(String json, Type type) {
        return JSON.parseObject(json, type);
    }

    public static Object deepClone(Object obj) {
        Class<? extends Object> clazz=obj.getClass();
        String json=JSON.toJSONString(obj);
        return JSON.parseObject(json, clazz);
    }

}
