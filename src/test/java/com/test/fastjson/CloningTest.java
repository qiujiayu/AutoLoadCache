package com.test.fastjson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jarvis.cache.clone.Cloning;
import com.jarvis.cache.clone.ICloner;

/**
 * @author: jiayu.qiu
 */
public class CloningTest {

    public static void main(String[] args) throws Exception {
        deepCloneTest();

        User user=new User();
        user.setId(2);
        user.setName("test2");
        user.setBirthday(new Date());
        ICloner s=new Cloning();
        User user2=(User)s.deepClone(user, User.class);
        user2.setName("test3333");
        System.out.println(user);
        System.out.println(user2);
    }

    private static void deepCloneTest() {
        List<User> list=new ArrayList<User>();
        User user=new User();
        user.setId(1);
        user.setName("test");
        user.setBirthday(new Date());
        list.add(user);
        Map<Integer, User> map=new HashMap<Integer, User>(4);
        map.put(user.getId(), user);
        Object[] arr=new Object[]{1, "test", list, map, User.class};
        ICloner s=new Cloning();
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
            System.out.println(obj.getClass().getName() + "--->" + obj);
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
