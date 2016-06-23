package com.jarvis.cache.serializer;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jarvis.cache.reflect.generics.ParameterizedTypeImpl;
import com.jarvis.cache.to.CacheWrapper;

public class FastjsonSerializer implements ISerializer<Object> {

    private final Charset charset;

    private static final SerializerFeature[] features={SerializerFeature.DisableCircularReferenceDetect};

    public FastjsonSerializer() {
        this(Charset.forName("UTF8"));
    }

    public FastjsonSerializer(Charset charset) {
        this.charset=charset;
    }

    @Override
    public byte[] serialize(final Object obj) throws Exception {
        if(obj == null) {
            return null;
        }
        String json=JSON.toJSONString(obj, features);
        return json.getBytes(charset);
    }

    @Override
    public Object deserialize(final byte[] bytes, final Type returnType) throws Exception {
        if(null == bytes || bytes.length == 0) {
            return null;
        }
        String json=new String(bytes, charset);
        Type[] agsType=new Type[]{returnType};

        return JSON.parseObject(json, ParameterizedTypeImpl.make(CacheWrapper.class, agsType, null));
    }

    @Override
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
        } else {
            String json=JSON.toJSONString(obj, features);
            return JSON.parseObject(json, clazz);
        }
    }

}
