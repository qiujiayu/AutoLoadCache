package com.jarvis.cache.serializer;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Simple String to byte[] (and back) serializer. Converts Strings into bytes and vice-versa using the specified charset (by default
 * UTF-8).
 * <p>
 * Useful when the interaction with the Redis happens mainly through Strings.
 * </p>
 * <p>
 * Does not perform any null conversion since empty strings are valid keys/values.
 * </p>
 */
public class StringSerializer implements ISerializer<String> {

    private final Charset charset;

    public StringSerializer() {
        this(Charset.forName("UTF8"));
    }

    public StringSerializer(Charset charset) {
        this.charset=charset;
    }

    @Override
    public String deserialize(byte[] bytes, Type returnType) throws Exception {
        return(bytes == null ? null : new String(bytes, charset));
    }

    @Override
    public byte[] serialize(String string) throws Exception {
        return(string == null ? null : string.getBytes(charset));
    }

    @Override
    public String deepClone(String obj) throws Exception {
        return deserialize(serialize(obj), null);
    }
}
