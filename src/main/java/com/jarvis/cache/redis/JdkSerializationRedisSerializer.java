package com.jarvis.cache.redis;

import com.jarvis.lib.util.BeanUtil;

public class JdkSerializationRedisSerializer implements RedisSerializer<Object> {

    public Object deserialize(byte[] bytes) throws Exception {
        if((bytes == null || bytes.length == 0)) {
            return null;
        }

        return BeanUtil.deserialize(bytes);
    }

    public byte[] serialize(Object object) throws Exception {
        if(object == null) {
            return new byte[0];
        }
        return BeanUtil.serialize(object);
    }
}
