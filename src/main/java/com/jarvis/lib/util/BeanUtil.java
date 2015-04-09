package com.jarvis.lib.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author jiayu.qiu
 */
public class BeanUtil {

    /**
     * 是否为基础数据类型
     * @param obj
     * @return
     */
    private static boolean isPrimitive(Object obj) {
        return obj.getClass().isPrimitive() || obj instanceof String || obj instanceof Integer || obj instanceof Long
            || obj instanceof Byte || obj instanceof Character || obj instanceof Boolean || obj instanceof Short
            || obj instanceof Float || obj instanceof Double || obj instanceof Date;
    }

    /**
     * 把Bean转换为字符串
     * @param obj
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static String toString(Object obj) {
        if(obj == null) {
            return "null";
        }
        Class cl=obj.getClass();
        if(isPrimitive(obj)) {
            return String.valueOf(obj);
        } else if(obj instanceof Enum) {
            return ((Enum)obj).name();
        } else if(cl.isArray()) {
            String r="[";
            for(int i=0; i < Array.getLength(obj); i++) {
                if(i > 0) {
                    r+=",";
                }
                Object val=Array.get(obj, i);
                if(null == val) {
                    r+="null";
                } else if(isPrimitive(val)) {
                    r+=String.valueOf(val);
                } else {
                    r+=toString(val);
                }
            }
            return r + "]";
        } else if(obj instanceof Collection) {
            Collection tempCol=(Collection)obj;
            Object[] tempArr=tempCol.toArray();
            String r="[";
            for(int i=0; i < tempArr.length; i++) {
                if(i > 0) {
                    r+=",";
                }
                Object val=tempArr[i];
                if(null == val) {
                    r+="null";
                } else if(isPrimitive(val)) {
                    r+=String.valueOf(val);
                } else {
                    r+=toString(val);
                }
            }
            return r + "]";
        } else if(obj instanceof Map) {
            Map tempMap=(Map)obj;
            String r="{";
            Iterator it=tempMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry entry=(Entry)it.next();
                if(it.hasNext()) {
                    r+=",";
                }
                Object key=entry.getKey();
                if(null == key) {
                    r+="null";
                } else if(isPrimitive(key)) {
                    r+=String.valueOf(key);
                } else {
                    r+=toString(key);
                }
                r+="=";
                Object val=entry.getValue();
                if(null == val) {
                    r+="null";
                } else if(isPrimitive(val)) {
                    r+=String.valueOf(val);
                } else {
                    r+=toString(val);
                }
            }
            return r + "}";
        }
        String r=cl.getName();
        do {
            Field[] fields=cl.getDeclaredFields();
            AccessibleObject.setAccessible(fields, true);
            if(null == fields || fields.length == 0) {
                cl=cl.getSuperclass();
                continue;
            }
            r+="[";
            // get the names and values of all fields
            for(Field f: fields) {
                if(!Modifier.isStatic(f.getModifiers())) {
                    if(f.isSynthetic() || f.getName().indexOf("this$") != -1) {
                        continue;
                    }
                    r+=f.getName() + "=";
                    try {
                        Object val=f.get(obj);
                        if(null == val) {
                            r+="null";
                        } else if(isPrimitive(val)) {
                            r+=String.valueOf(val);
                        } else {
                            r+=toString(val);
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    r+=",";
                }
            }
            if(r.endsWith(",")) {
                r=r.substring(0, r.length() - 1);
            }
            r+="]";
            cl=cl.getSuperclass();
        } while(cl != null);
        return r;
    }

    /**
     * 通过序列化进行深度复制
     * @param obj
     * @return
     * @throws Exception
     */
    public static Object deepClone(Object obj) throws Exception {
        return deserialize(serialize(obj));
    }

    public static byte[] serialize(Object obj) throws IOException {
        if(null == obj) {
            return null;
        }
        // 将对象写到流里
        ByteArrayOutputStream bo=new ByteArrayOutputStream();
        ObjectOutputStream oo=new ObjectOutputStream(bo);
        oo.writeObject(obj);
        oo.flush();
        return bo.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws Exception {
        if(null == bytes || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream bi=new ByteArrayInputStream(bytes);
        ObjectInputStream oi=new ObjectInputStream(bi);
        return oi.readObject();
    }
}
