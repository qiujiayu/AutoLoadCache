package com.jarvis.cache.serializer;

import com.jarvis.cache.serializer.kryo.CacheWrapperSerializer;
import com.jarvis.cache.serializer.kryo.DefaultKryoContext;
import com.jarvis.cache.serializer.kryo.KryoContext;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

/**
 * autoload cache kryo serializer
 *
 * @author stevie.wong
 */
@Slf4j
public class KryoSerializer implements ISerializer<Object> {
    private KryoContext kryoContext;

    public KryoSerializer() {
        this.kryoContext = DefaultKryoContext.newKryoContextFactory(kryo -> {
            kryo.register(CacheWrapper.class, new CacheWrapperSerializer());
            if(log.isDebugEnabled()) {
                log.debug("kryo register classes successfully.");
            }
        });
    }


    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj == null) {
            return null;
        }

        return kryoContext.serialize(obj);
    }

    @Override
    public Object deserialize(byte[] bytes, Type returnType) throws Exception {
        if (null == bytes || bytes.length == 0) {
            return null;
        }

        return kryoContext.deserialize(bytes);
    }

    @Override
    public Object deepClone(Object obj, Type type) throws Exception {
        if (null == obj) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        if (BeanUtil.isPrimitive(obj) || clazz.isEnum() || obj instanceof Class || clazz.isAnnotation() || clazz.isSynthetic()) {// 常见不会被修改的数据类型
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
            throw new Exception("the length of " + method.getDeclaringClass().getName() + "." + method.getName() + " must " + genericParameterTypes.length);
        }
        Object[] res = new Object[args.length];
        int len = genericParameterTypes.length;
        for (int i = 0; i < len; i++) {
            res[i] = deepClone(args[i], null);
        }
        return res;
    }
}
