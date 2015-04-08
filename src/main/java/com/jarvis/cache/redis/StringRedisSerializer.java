package com.jarvis.cache.redis;

import java.nio.charset.Charset;

/**
 * Simple String to byte[] (and back) serializer. Converts Strings into bytes and vice-versa using the specified charset (by default
 * UTF-8).
 * <p/>
 * Useful when the interaction with the Redis happens mainly through Strings.
 * <p/>
 * Does not perform any null conversion since empty strings are valid keys/values.
 */
public class StringRedisSerializer implements RedisSerializer<String> {

    private final Charset charset;

    public StringRedisSerializer() {
        this(Charset.forName("UTF8"));
    }

    public StringRedisSerializer(Charset charset) {
        this.charset=charset;
    }

    public String deserialize(byte[] bytes) {
        return(bytes == null ? null : new String(bytes, charset));
    }

    public byte[] serialize(String string) {
        return(string == null ? null : string.getBytes(charset));
    }
}
