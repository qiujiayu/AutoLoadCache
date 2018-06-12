package com.jarvis.lib.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiayu.qiu
 */
public class BeanUtil {

    @SuppressWarnings("rawtypes")
    private static final ConcurrentHashMap<Class, Field[]> FIELDS_CAHCE = new ConcurrentHashMap<Class, Field[]>();

    /**
     * 是否为基础数据类型
     * 
     * @param obj Object
     * @return boolean true or false
     */
    public static boolean isPrimitive(Object obj) {
        boolean rv = obj.getClass().isPrimitive() || obj instanceof String || obj instanceof Integer
                || obj instanceof Long || obj instanceof Byte || obj instanceof Character || obj instanceof Boolean
                || obj instanceof Short || obj instanceof Float || obj instanceof Double || obj instanceof BigDecimal
                || obj instanceof BigInteger;
        return rv;
    }

    /**
     * 把Bean转换为字符串
     * 
     * @param obj Object
     * @return String String
     */
    @SuppressWarnings("rawtypes")
    public static String toString(Object obj) {
        if (obj == null) {
            return "null";
        }
        Class cl = obj.getClass();
        if (isPrimitive(obj)) {
            return String.valueOf(obj);
        } else if (obj instanceof Enum) {
            return ((Enum) obj).name();
        } else if (obj instanceof Date) {
            return String.valueOf(((Date) obj).getTime());
        } else if (obj instanceof Calendar) {
            return String.valueOf(((Calendar) obj).getTime().getTime());
        } else if (cl.isArray()) {
            String r = "[";
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    r += ",";
                }
                Object val = Array.get(obj, i);
                r += toString(val);
            }
            return r + "]";
        } else if (obj instanceof Collection) {
            Collection tempCol = (Collection) obj;
            Iterator it = tempCol.iterator();
            String r = "[";
            for (int i = 0; it.hasNext(); i++) {
                if (i > 0) {
                    r += ",";
                }
                Object val = it.next();
                r += toString(val);
            }
            return r + "]";
        } else if (obj instanceof Map) {
            Map tempMap = (Map) obj;
            String r = "{";
            Iterator it = tempMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Entry) it.next();
                Object key = entry.getKey();
                r += toString(key);
                r += "=";
                Object val = entry.getValue();
                r += toString(val);
                if (it.hasNext()) {
                    r += ",";
                }
            }
            return r + "}";
        } else if (obj instanceof Class) {
            Class tmpCls = (Class) obj;
            return tmpCls.getName();
        }
        String r = cl.getName();
        do {
            Field[] fields = FIELDS_CAHCE.get(cl);
            if (null == fields) {
                fields = cl.getDeclaredFields();
                if (null != fields) {
                    AccessibleObject.setAccessible(fields, true);
                }
                FIELDS_CAHCE.put(cl, fields);
            }
            if (null == fields || fields.length == 0) {
                cl = cl.getSuperclass();
                continue;
            }
            r += "[";
            // get the names and values of all fields
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (f.isSynthetic() || f.getName().indexOf("this$") != -1) {
                    continue;
                }
                r += f.getName() + "=";
                try {
                    Object val = f.get(obj);
                    r += toString(val);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                r += ",";

            }
            String comma = ",";
            if (r.endsWith(comma)) {
                r = r.substring(0, r.length() - 1);
            }
            r += "]";
            cl = cl.getSuperclass();
        } while (cl != null);
        return r;
    }

}
