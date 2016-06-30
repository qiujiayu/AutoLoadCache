package com.test.fastjson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.cglib.beans.BeanCopier;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jarvis.cache.clone.Cloning;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.serializer.FastjsonSerializer;
import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.serializer.JdkSerializer;
import com.test.Stopwatch;

public class DeepCloneTest {

    private static final SerializerFeature[] features={SerializerFeature.DisableCircularReferenceDetect};

    private static int hot=10000;

    private static int run=100000;

    public static void main(String[] args) throws Exception {
        List<User> list=new ArrayList<User>();
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        list.add(user);
        BeanCopier copy=BeanCopier.create(user.getClass(), user.getClass(), false);
        User user2=User.class.newInstance();
        user.setName("testaaaa");
        copy.copy(user, user2, null);
        System.out.println(user2);

        List list1=list.getClass().newInstance();
        Collections.addAll(list1, list);
        user.setName("test1111");
        Collections.copy(list1, list);

        System.out.println(list1);

        Type type1=list.getClass().getGenericSuperclass();
        if(type1 instanceof ParameterizedType) {
            ParameterizedType type=((ParameterizedType)type1);
            System.out.println(type);
        } else {
            System.out.println("type2 is not ParameterizedType");
        }
        List list2=new ArrayList();
        Type type2=list2.getClass().getGenericSuperclass();
        if(type2 instanceof ParameterizedType) {
            ParameterizedType type=((ParameterizedType)type2);
            System.out.println(type.getActualTypeArguments()[0].getClass().getName());
        } else {
            System.out.println("type2 is not ParameterizedType");
        }
        // test1();
        // fastJsonTest();
        deepClone(new JdkSerializer());
        deepClone(new HessianSerializer());
        deepClone(new FastjsonSerializer());
        deepClone(new Cloning());
    }

    private static void test1() {
        List<User> list=new ArrayList<User>();
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        list.add(user);
        Map<Integer, User> map=new HashMap<Integer, User>();
        map.put(user.getId(), user);
        Object[] arr=new Object[]{1, "test", list, map, User.class};
        String json=JSON.toJSONString(arr, features);
        System.out.println(json);
        Object[] arr2=JSON.parseObject(json, arr.getClass());
        for(int i=0; i < arr2.length; i++) {
            Object obj=arr2[i];
            System.out.println(obj.getClass().getName() + "--->" + obj);
        }
    }

    private static void fastJsonTest() {
        List<User> list=new ArrayList<User>();
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        list.add(user);
        Map<Integer, User> map=new HashMap<Integer, User>();
        map.put(user.getId(), user);
        Object[] arr=new Object[]{1, "test", list, map, User.class};
        ISerializer<Object> s=new FastjsonSerializer();
        try {
            System.out.println("--------------obj arr------------------");
            Object[] rs=(Object[])s.deepClone(arr, null);
            for(int i=0; i < rs.length; i++) {
                Object obj=rs[i];
                System.out.println(obj.getClass().getName() + "--->" + obj);
            }
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("--------------user arr------------------");
        User[] arr2=new User[]{user};
        try {
            Object[] rs=(Object[])s.deepClone(arr2, null);
            for(int i=0; i < rs.length; i++) {
                Object obj=rs[i];
                System.out.println(obj.getClass().getName() + "--->" + obj);
            }
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("--------------map------------------");
        try {
            Map<Integer, User> obj=(Map<Integer, User>)s.deepClone(map, null);
            Iterator<Map.Entry<Integer, User>> it=obj.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<Integer, User> enty=it.next();
                Object key=enty.getKey();
                Object val=enty.getValue();
                System.out.println(key.getClass().getName() + "--->" + key);
                System.out.println(val.getClass().getName() + "--->" + val);
            }
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static Object getUser() {
        List<User> list=new ArrayList<User>();
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        list.add(user);
        Map<Integer, User> map=new HashMap<Integer, User>();
        User user2=new User();
        user2.setId(1);
        user2.setName("test");
        user2.setBirthday(new Date());
        map.put(user2.getId(), user2);
        return new Object[]{1, "test", list, map, user, new String[]{"aaa", "bbb"}};
    }

    public static void deepClone(ICloner h) throws Exception {
        Object user=getUser();

        for(int i=0; i < hot; i++) {
            h.deepClone(user, null);
        }

        Stopwatch sw=Stopwatch.begin();
        for(int i=0; i < run; i++) {
            h.deepClone(user, null);
        }
        sw.stop();
        System.out.println(h.getClass().getName() + ": " + sw);
    }
}
