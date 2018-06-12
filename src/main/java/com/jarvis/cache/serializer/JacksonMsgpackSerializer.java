package com.jarvis.cache.serializer;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarvis.cache.reflect.generics.ParameterizedTypeImpl;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;

/**
 * 基于msgpack 和 Jackson进行处理
 * 
 * @author jiayu.qiu
 */
public class JacksonMsgpackSerializer implements ISerializer<Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper(new MessagePackFactory());

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj == null) {
            return null;
        }
        return MAPPER.writeValueAsBytes(obj);
    }

    @Override
    public Object deserialize(byte[] bytes, Type returnType) throws Exception {
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        Type[] agsType = new Type[] { returnType };
        JavaType javaType = MAPPER.getTypeFactory()
                .constructType(ParameterizedTypeImpl.make(CacheWrapper.class, agsType, null));
        return MAPPER.readValue(bytes, javaType);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object deepClone(Object obj, final Type type) throws Exception {
        if (null == obj) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        if (BeanUtil.isPrimitive(obj) || clazz.isEnum() || obj instanceof Class || clazz.isAnnotation()
                || clazz.isSynthetic()) {// 常见不会被修改的数据类型
            return obj;
        }
        if (obj instanceof Date) {
            return ((Date) obj).clone();
        } else if (obj instanceof Calendar) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(((Calendar) obj).getTime().getTime());
            return cal;
        }
        if (null != type) {
            byte[] tmp = MAPPER.writeValueAsBytes(obj);
            JavaType javaType = MAPPER.getTypeFactory().constructType(type);
            return MAPPER.readValue(tmp, javaType);
        }

        if (clazz.isArray()) {
            Object[] arr = (Object[]) obj;

            Object[] res = ((Object) clazz == (Object) Object[].class) ? (Object[]) new Object[arr.length]
                    : (Object[]) Array.newInstance(clazz.getComponentType(), arr.length);
            for (int i = 0; i < arr.length; i++) {
                res[i] = deepClone(arr[i], null);
            }
            return res;
        } else if (obj instanceof Collection) {
            Collection<?> tempCol = (Collection<?>) obj;
            Collection res = tempCol.getClass().newInstance();

            Iterator<?> it = tempCol.iterator();
            while (it.hasNext()) {
                Object val = deepClone(it.next(), null);
                res.add(val);
            }
            return res;
        } else if (obj instanceof Map) {
            Map tempMap = (Map) obj;
            Map res = tempMap.getClass().newInstance();
            Iterator it = tempMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Entry) it.next();
                Object key = entry.getKey();
                Object val = entry.getValue();
                res.put(deepClone(key, null), deepClone(val, null));
            }
            return res;
        } else if (obj instanceof CacheWrapper) {
            CacheWrapper<Object> wrapper = (CacheWrapper<Object>) obj;
            CacheWrapper<Object> res = new CacheWrapper<Object>();
            res.setExpire(wrapper.getExpire());
            res.setLastLoadTime(wrapper.getLastLoadTime());
            res.setCacheObject(deepClone(wrapper.getCacheObject(), null));
            return res;
        } else {
            byte[] tmp = MAPPER.writeValueAsBytes(obj);
            return MAPPER.readValue(tmp, clazz);
        }
    }

    @Override
    public Object[] deepCloneMethodArgs(Method method, Object[] args) throws Exception {
        if (null == args || args.length == 0) {
            return args;
        }
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (args.length != genericParameterTypes.length) {
            throw new Exception("the length of " + method.getDeclaringClass().getName() + "." + method.getName()
                    + " must " + genericParameterTypes.length);
        }
        Class<?> clazz = args.getClass();
        Object[] res = ((Object) clazz == (Object) Object[].class) ? (Object[]) new Object[args.length]
                : (Object[]) Array.newInstance(clazz.getComponentType(), args.length);
        int len = genericParameterTypes.length;
        for (int i = 0; i < len; i++) {
            Type genericParameterType = genericParameterTypes[i];
            Object obj = args[i];
            if (genericParameterType instanceof ParameterizedType) {
                byte[] tmp = MAPPER.writeValueAsBytes(obj);
                JavaType javaType = MAPPER.getTypeFactory().constructType(genericParameterType);
                res[i] = MAPPER.readValue(tmp, javaType);
            } else {
                res[i] = deepClone(obj, null);
            }
        }
        return res;
    }

}
