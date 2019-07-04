package com.jarvis.cache.serializer.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.jarvis.cache.reflect.lambda.Lambda;
import com.jarvis.cache.reflect.lambda.LambdaFactory;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhengenshen@gmail.com
 */
@Slf4j
public class ProtoBufSerializer implements ISerializer<CacheWrapper<Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final byte MESSAGE_TYPE = 0;
    private static final byte JSON_TYPE = 1;

    private ConcurrentHashMap<Class, Lambda> lambdaMap = new ConcurrentHashMap<>(64);

    private ObjectPool<WriteByteBuf> writePool;
    private ObjectPool<ReadByteBuf> readPool;

    public ProtoBufSerializer() {
        init();
    }

    private void init() {
        writePool = new GenericObjectPool<>(CacheObjectFactory.createWriteByteBuf());
        readPool = new GenericObjectPool<>(CacheObjectFactory.createReadByteBuf());
    }

    @Override
    public byte[] serialize(CacheWrapper<Object> obj) throws Exception {
        val byteBuf = writePool.borrowObject();
        byteBuf.resetIndex();
        byteBuf.writeInt(obj.getExpire());
        byteBuf.writeLong(obj.getLastLoadTime());
        Object cacheObj = obj.getCacheObject();

        if (cacheObj instanceof Message) {
            Message message = (Message) cacheObj;
            byteBuf.writeByte(MESSAGE_TYPE);
            message.writeTo(new ByteBufOutputStream(byteBuf));
        } else if (cacheObj != null) {
            byteBuf.writeByte(JSON_TYPE);
            byteBuf.writeBytes(MAPPER.writeValueAsBytes(cacheObj));
        }
        val bytes = byteBuf.readableBytes();
        writePool.returnObject(byteBuf);
        return bytes;
    }


    @Override
    public CacheWrapper<Object> deserialize(byte[] bytes, Type returnType) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        CacheWrapper<Object> cacheWrapper = new CacheWrapper<>();
        val byteBuf = readPool.borrowObject();
        byteBuf.setBytes(bytes);
        cacheWrapper.setExpire(byteBuf.readInt());
        cacheWrapper.setLastLoadTime(byteBuf.readLong());
        byte type = byteBuf.readByte();
        bytes = byteBuf.readableBytes();
        readPool.returnObject(byteBuf);
        if (bytes == null || bytes.length == 0) {
            return cacheWrapper;
        }
        switch (type) {
            case MESSAGE_TYPE:
                String typeName;
                //处理泛型 Magic模式下 returnType 肯定是泛型的，需要做特殊处理
                if (returnType instanceof ParameterizedType) {
                    typeName = ((ParameterizedType) returnType).getActualTypeArguments()[0].getTypeName();
                } else {
                    typeName = returnType.getTypeName();
                }

                Class clazz = Class.forName(typeName);
                if (Message.class.isAssignableFrom(clazz)) {
                    Lambda lambda = getLambda(clazz);
                    Object obj = lambda.invoke_for_Object(new ByteArrayInputStream(bytes));
                    cacheWrapper.setCacheObject(obj);
                }
                break;
            case JSON_TYPE:
                cacheWrapper.setCacheObject(MAPPER.readValue(bytes, MAPPER.constructType(returnType)));
                break;
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
        if (BeanUtil.isPrimitive(obj) || clazz.isEnum() || obj instanceof Class || clazz.isAnnotation() || clazz.isSynthetic()) {
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

            Object[] res = (clazz == Object[].class) ? new Object[arr.length]
                    : (Object[]) Array.newInstance(clazz.getComponentType(), arr.length);
            for (int i = 0; i < arr.length; i++) {
                res[i] = deepClone(arr[i], null);
            }
            return res;
        }
        if (obj instanceof Collection) {
            Collection<?> tempCol = (Collection<?>) obj;
            Collection res = tempCol.getClass().newInstance();

            Iterator<?> it = tempCol.iterator();
            while (it.hasNext()) {
                Object val = deepClone(it.next(), null);
                res.add(val);
            }
            return res;
        }
        if (obj instanceof Map) {
            Map tempMap = (Map) obj;
            Map res = tempMap.getClass().newInstance();
            Iterator it = tempMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                Object val = entry.getValue();
                res.put(deepClone(key, null), deepClone(val, null));
            }
            return res;
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
