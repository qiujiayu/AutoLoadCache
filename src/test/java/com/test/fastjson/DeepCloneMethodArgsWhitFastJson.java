package com.test.fastjson;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.test.Stopwatch;

/**
 * 使用FastJson进行深度复制 Method中的参数
 * @author jiayu.qiu
 */
public class DeepCloneMethodArgsWhitFastJson {

    private static final SerializerFeature[] features={SerializerFeature.DisableCircularReferenceDetect};

    private static int hot=10000;

    private static int run=100000;

    @Test
    public void testDeepClone() throws Exception {
        Method methods[]=DeepCloneMethodArgsWhitFastJson.class.getDeclaredMethods();
        String testName="getUserList";
        Method method=null;
        for(Method t: methods) {
            if(t.getName().equals(testName)) {
                method=t;
                break;
            }
        }
        assertNotNull(method);
        Object[] args=getArgs();
        Object[] res=deepClone(method, args);
        for(Object obj: res) {
            System.out.println(obj.getClass().getName() + "---->" + obj);
        }
        for(int i=0; i < hot; i++) {
            deepClone(method, args);
        }

        Stopwatch sw=Stopwatch.begin();
        for(int i=0; i < run; i++) {
            deepClone(method, args);
        }
        sw.stop();
        System.out.println("DeepCloneMethodArgsWhitFastJson: " + sw);

    }

    public Object[] deepClone(Method method, Object[] args) throws Exception {
        if(null == args || args.length == 0) {
            return args;
        }
        Type[] genericParameterTypes=method.getGenericParameterTypes();
        if(args.length != genericParameterTypes.length) {
            throw new Exception("the length of " + method.getDeclaringClass().getName() + "." + method.getName() + " must "
                + genericParameterTypes.length);
        }
        Class<?> clazz=args.getClass();
        Object[] res=
            ((Object)clazz == (Object)Object[].class) ? (Object[])new Object[args.length] : (Object[])Array.newInstance(
                clazz.getComponentType(), args.length);
        int len=genericParameterTypes.length;
        for(int i=0; i < len; i++) {
            Type genericParameterType=genericParameterTypes[i];
            Object obj=args[i];
            if(genericParameterType instanceof ParameterizedType) {
                String json=JSON.toJSONString(obj, features);
                res[i]=JSON.parseObject(json, genericParameterType);
            } else {
                res[i]=deepClone(obj);
            }
        }
        return res;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object deepClone(Object obj) throws Exception {
        if(null == obj) {
            return null;
        }
        Class<?> clazz=obj.getClass();
        if(clazz.isArray()) {
            Object[] arr=(Object[])obj;

            Object[] res=
                ((Object)clazz == (Object)Object[].class) ? (Object[])new Object[arr.length] : (Object[])Array.newInstance(
                    clazz.getComponentType(), arr.length);
            for(int i=0; i < arr.length; i++) {
                res[i]=deepClone(arr[i]);
            }
            return res;
        } else if(obj instanceof Collection) {
            Collection<?> tempCol=(Collection<?>)obj;
            Collection res=tempCol.getClass().newInstance();

            Iterator<?> it=tempCol.iterator();
            while(it.hasNext()) {
                Object val=deepClone(it.next());
                res.add(val);
            }
            return res;
        } else if(obj instanceof Map) {
            Map tempMap=(Map)obj;
            Map res=tempMap.getClass().newInstance();
            Iterator it=tempMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry entry=(Entry)it.next();
                Object key=entry.getKey();
                Object val=entry.getValue();
                res.put(deepClone(key), deepClone(val));
            }
            return res;

        } else {
            String json=JSON.toJSONString(obj, features);
            return JSON.parseObject(json, clazz);
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
