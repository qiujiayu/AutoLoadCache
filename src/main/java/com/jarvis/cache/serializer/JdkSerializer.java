package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

import com.jarvis.lib.util.BeanUtil;

/**
 * @author: jiayu.qiu
 */
public class JdkSerializer implements ISerializer<Object> {

    @Override
    public Object deserialize(byte[] bytes, Type returnType) throws Exception {
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream input = new ObjectInputStream(inputStream);
        return input.readObject();
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj == null) {
            return new byte[0];
        }
        // 将对象写到流里
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(outputStream);
        output.writeObject(obj);
        output.flush();
        return outputStream.toByteArray();
    }

    @Override
    public Object deepClone(Object obj, final Type type) throws Exception {
        if (null == obj) {
            return obj;
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
        return deserialize(serialize(obj), null);
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
        Object[] res = new Object[args.length];
        int len = genericParameterTypes.length;
        for (int i = 0; i < len; i++) {
            res[i] = deepClone(args[i], null);
        }
        return res;
    }
}
