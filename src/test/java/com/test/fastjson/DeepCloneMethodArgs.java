package com.test.fastjson;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.jarvis.cache.clone.Cloning;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.serializer.FastjsonSerializer;
import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.serializer.JacksonJsonSerializer;
import com.jarvis.cache.serializer.JacksonMsgpackSerializer;
import com.jarvis.cache.serializer.JdkSerializer;
import com.test.Stopwatch;

/**
 * 使用深度复制 Method中的参数
 * @author jiayu.qiu
 */
public class DeepCloneMethodArgs {

    private static int hot=10000;

    private static int run=100000;

    @Test
    public void testDeepClone() throws Exception {
        Method methods[]=DeepCloneMethodArgs.class.getDeclaredMethods();
        String testName="getUserList";
        Method method=null;
        for(Method m: methods) {
            if(m.getName().equals(testName)) {
                method=m;
                break;
            }
        }

        assertNotNull(method);
        Object[] args=getArgs();
        testSerializer(new JdkSerializer(), method, args);
        testSerializer(new HessianSerializer(), method, args);
        testSerializer(new FastjsonSerializer(), method, args);
        testSerializer(new JacksonJsonSerializer(), method, args);
        testSerializer(new JacksonMsgpackSerializer(), method, args);
        testSerializer(new Cloning(), method, args);
    }

    private void testSerializer(ICloner cloner, Method method, Object[] args) throws Exception {
        Object[] res=cloner.deepCloneMethodArgs(method, args);
        printObject(res);
        for(int i=0; i < hot; i++) {
            cloner.deepCloneMethodArgs(method, args);
        }

        Stopwatch sw=Stopwatch.begin();
        for(int i=0; i < run; i++) {
            cloner.deepCloneMethodArgs(method, args);
        }
        sw.stop();
        System.out.println(cloner.getClass().getName() + "--->" + sw);
    }

    private void printObject(Object[] res) {
        for(Object obj: res) {
            Class<?> clazz=obj.getClass();
            if(clazz.isArray()) {
                System.out.print(obj.getClass().getName() + "---->[");
                Object[] arr=(Object[])obj;
                for(Object t: arr) {
                    System.out.print(t + ",");
                }
                System.out.println("]");
            } else {
                System.out.println(obj.getClass().getName() + "---->" + obj);
            }
        }
    }

    public List<User> getUserList(Integer id, String name, List<User> list, Map<Integer, User> map, User user, String[] args) {
        return null;
    }

    private static Object[] getArgs() {
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
        // return new Object[]{1, "test", list};
        // return new User[]{user};
        // return map;
    }
}
