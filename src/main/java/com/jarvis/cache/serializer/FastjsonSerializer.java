package com.jarvis.cache.serializer;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;

public class FastjsonSerializer implements ISerializer<Object> {

    private final Charset charset;

    public FastjsonSerializer() {
        this(Charset.forName("UTF8"));
    }

    public FastjsonSerializer(Charset charset) {
        this.charset=charset;
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if(obj == null) {
            return null;
        }
        String json=JSON.toJSONString(obj);
        return json.getBytes(charset);
    }

    @Override
    public Object deserialize(byte[] bytes, Type returnType) throws Exception {
        if(null == bytes || bytes.length == 0) {
            return null;
        }
        String json=new String(bytes, charset);
        return JSON.parseObject(json, returnType);
    }

    @Override
    public Object deepClone(Object obj) throws Exception {
        Class<? extends Object> clazz=obj.getClass();
        String json=JSON.toJSONString(obj);
        return JSON.parseObject(json, clazz);
    }

}
