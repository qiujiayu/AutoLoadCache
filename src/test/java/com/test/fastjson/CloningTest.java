package com.test.fastjson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jarvis.cache.clone.Cloning;
import com.jarvis.cache.clone.ICloner;

public class CloningTest {

    public static void main(String[] args) {
        deepCloneTest();

    }

    private static void deepCloneTest() {
        List<User> list=new ArrayList<User>();
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        list.add(user);
        Map<Integer, User> map=new HashMap<Integer, User>();
        map.put(user.getId(), user);
        Object[] arr=new Object[]{1, "test", list, map, User.class};
        ICloner s=new Cloning();
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
}
