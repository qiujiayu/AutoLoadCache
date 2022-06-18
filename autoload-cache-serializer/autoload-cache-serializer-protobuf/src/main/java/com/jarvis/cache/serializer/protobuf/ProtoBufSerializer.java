package com.jarvis.cache.serializer.protobuf;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.jarvis.cache.reflect.generics.ParameterizedTypeImpl;
import com.jarvis.cache.reflect.lambda.Lambda;
import com.jarvis.cache.reflect.lambda.LambdaFactory;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;
import com.jarvis.lib.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ProtoBufSerializer() {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.registerModule(new SimpleModule().addSerializer(new NullValueSerializer(null)));
        MAPPER.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }

    @Override
    public byte[] serialize(CacheWrapper<Object> obj) throws Exception {
        WriteByteBuf byteBuf = new WriteByteBuf();
        byteBuf.writeInt(obj.getExpire());
        byteBuf.writeLong(obj.getLastLoadTime());
        Object cacheObj = obj.getCacheObject();
        if (cacheObj != null) {
            if (cacheObj instanceof Message) {
                byteBuf.writeBytes(((Message) cacheObj).toByteArray());
            } else {
                MAPPER.writeValue(byteBuf, cacheObj);
            }
        }
        return byteBuf.toByteArray();
    }


    @Override
    public CacheWrapper<Object> deserialize(final byte[] bytes, Type returnType) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        CacheWrapper<Object> cacheWrapper = new CacheWrapper<>();
        val byteBuf = new ReadByteBuf(bytes);
        cacheWrapper.setExpire(byteBuf.readInt());
        cacheWrapper.setLastLoadTime(byteBuf.readLong());
        val body = byteBuf.readableBytes();
        if (body == null || body.length == 0) {
            return cacheWrapper;
        }
        Class<?> clazz = TypeFactory.rawClass(returnType);
        if (Message.class.isAssignableFrom(clazz)) {
            Lambda lambda = getLambda(clazz);
            Object obj = lambda.invoke_for_Object(new ByteArrayInputStream(body));
            cacheWrapper.setCacheObject(obj);
        } else {
            Type[] agsType = new Type[]{returnType};
            JavaType javaType = MAPPER.getTypeFactory().constructType(ParameterizedTypeImpl.make(CacheWrapper.class, agsType, null));
            cacheWrapper.setCacheObject(MAPPER.readValue(body, clazz));
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
        return MAPPER.readValue(MAPPER.writeValueAsBytes(obj), clazz);
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

    private class NullValueSerializer extends StdSerializer<NullValue> {

        private static final long serialVersionUID = 1999052150548658808L;

        private final String classIdentifier;

        /**
         * @param classIdentifier can be {@literal null} and will be defaulted
         *                        to {@code @class}.
         */
        NullValueSerializer(String classIdentifier) {

            super(NullValue.class);
            this.classIdentifier = StringUtil.hasText(classIdentifier) ? classIdentifier : "@class";
        }

        /*
         * (non-Javadoc)
         * @see
         * com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.
         * lang.Object, com.fasterxml.jackson.core.JsonGenerator,
         * com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

            jgen.writeStartObject();
            jgen.writeStringField(classIdentifier, NullValue.class.getName());
            jgen.writeEndObject();
        }
    }
}