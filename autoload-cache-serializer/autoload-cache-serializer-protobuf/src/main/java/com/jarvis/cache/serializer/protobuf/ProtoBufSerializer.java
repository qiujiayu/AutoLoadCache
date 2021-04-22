package com.jarvis.cache.serializer.protobuf;

import com.google.protobuf.Message;
import com.jarvis.cache.reflect.lambda.Lambda;
import com.jarvis.cache.reflect.lambda.LambdaFactory;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhengenshen@gmail.com
 */
@Slf4j
public class ProtoBufSerializer implements ISerializer<CacheWrapper<Object>> {

    private ConcurrentHashMap<Class, Lambda> lambdaMap = new ConcurrentHashMap<>();

    @Override
    public byte[] serialize(CacheWrapper<Object> obj) {
        WriteByteBuf byteBuf = new WriteByteBuf();
        byteBuf.writeInt(obj.getExpire());
        byteBuf.writeLong(obj.getLastLoadTime());
        Object cacheObj = obj.getCacheObject();
        if (cacheObj != null) {
            if (cacheObj instanceof Message) {
                byteBuf.writeBytes(((Message) cacheObj).toByteArray());
            } else {
                SerializationUtils.serialize((Serializable) cacheObj, byteBuf);
            }
        }
        return byteBuf.toByteArray();
    }


    @Override
    public CacheWrapper<Object> deserialize(final byte[] bytes, Type returnType) throws Exception {
        if (ArrayUtils.isEmpty(bytes)) {
            return null;
        }
        CacheWrapper<Object> cacheWrapper = new CacheWrapper<>();
        val byteBuf = new ReadByteBuf(bytes);
        cacheWrapper.setExpire(byteBuf.readInt());
        cacheWrapper.setLastLoadTime(byteBuf.readLong());
        val body = byteBuf.readableBytes();
        if (ArrayUtils.isEmpty(body)) {
            return cacheWrapper;
        }
        String typeName = null;
        if (!(returnType instanceof ParameterizedType)) {
            typeName = returnType.getTypeName();
        }
        Class clazz = ClassUtils.getClass(typeName);
        if (Message.class.isAssignableFrom(clazz)) {
            Lambda lambda = getLambda(clazz);
            Object obj = lambda.invoke_for_Object(new ByteArrayInputStream(body));
            cacheWrapper.setCacheObject(obj);
        } else {
            cacheWrapper.setCacheObject(SerializationUtils.deserialize(body));
        }
        return cacheWrapper;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object deepClone(Object obj, Type type) throws Exception {
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
        if (obj instanceof CacheWrapper) {
            CacheWrapper<Object> wrapper = (CacheWrapper<Object>) obj;
            CacheWrapper<Object> res = new CacheWrapper<>();
            res.setExpire(wrapper.getExpire());
            res.setLastLoadTime(wrapper.getLastLoadTime());
            res.setCacheObject(deepClone(wrapper.getCacheObject(), null));
            return res;
        }
        if (obj instanceof Message) {
            return ((Message) obj).toBuilder().build();
        }

        return ObjectUtils.clone(obj);
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

    @SuppressWarnings("unchecked")
    private Lambda getLambda(Class clazz) throws NoSuchMethodException {
        Lambda lambda = lambdaMap.get(clazz);
        if (lambda == null) {
            Method method = clazz.getDeclaredMethod("parseFrom", InputStream.class);
            try {
                lambda = LambdaFactory.create(method);
                lambdaMap.put(clazz, lambda);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return lambda;
    }
}
