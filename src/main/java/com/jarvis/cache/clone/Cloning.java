package com.jarvis.cache.clone;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

import com.jarvis.lib.util.BeanUtil;
import com.rits.cloning.Cloner;

/**
 * @author: jiayu.qiu
 */
public class Cloning implements ICloner {

    private final Cloner cloner = new Cloner();

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
        if (clazz.isArray()) {
            Object[] arr = (Object[]) obj;

            Object[] res = ((Object) clazz == (Object) Object[].class) ? (Object[]) new Object[arr.length]
                    : (Object[]) Array.newInstance(clazz.getComponentType(), arr.length);
            for (int i = 0; i < arr.length; i++) {
                res[i] = deepClone(arr[i], null);
            }
            return res;
        }
        return cloner.deepClone(obj);
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
