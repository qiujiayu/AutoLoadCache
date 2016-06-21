package com.test.fastjson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jarvis.cache.clone.Cloning;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.serializer.FastjsonSerializer;
import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.serializer.JdkSerializer;
import com.test.Stopwatch;

public class DeepCloneTest {

    private static int hot=10000;

    private static int run=100000;

    public static void main(String[] args) throws Exception {
        // fastJsonTest();
        serializerDeepClone(new JdkSerializer());
        serializerDeepClone(new HessianSerializer());
        serializerDeepClone(new FastjsonSerializer());
        deepClone(new Cloning());
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
            Object[] rs=(Object[])s.deepClone(arr);
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
            Object[] rs=(Object[])s.deepClone(arr2);
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
            Map<Integer, User> obj=(Map<Integer, User>)s.deepClone(map);
            System.out.println(obj.getClass().getName() + "--->" + obj);
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static User getUser() {
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        return user;
    }

    public static void serializerDeepClone(ISerializer<Object> h) throws Exception {
        User user=getUser();

        for(int i=0; i < hot; i++) {
            h.deepClone(user);
        }

        Stopwatch sw=Stopwatch.begin();
        for(int i=0; i < run; i++) {
            h.deepClone(user);
        }
        sw.stop();
        System.out.println(h.getClass().getName() + ": " + sw);
    }

    public static void deepClone(ICloner h) throws Exception {
        User user=getUser();

        for(int i=0; i < hot; i++) {
            h.deepClone(user);
        }

        Stopwatch sw=Stopwatch.begin();
        for(int i=0; i < run; i++) {
            h.deepClone(user);
        }
        sw.stop();
        System.out.println(h.getClass().getName() + ": " + sw);
    }
}
