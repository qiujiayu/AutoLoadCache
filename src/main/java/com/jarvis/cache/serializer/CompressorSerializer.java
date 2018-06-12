package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.jarvis.cache.compress.CommonsCompressor;
import com.jarvis.cache.compress.ICompressor;

/**
 * memcache缓存管理
 * 
 * @author: jiayu.qiu
 */
public class CompressorSerializer implements ISerializer<Object> {

    private static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

    private int compressionThreshold = DEFAULT_COMPRESSION_THRESHOLD;

    private final ISerializer<Object> serializer;

    private final ICompressor compressor;

    public CompressorSerializer(ISerializer<Object> serializer) {
        this.serializer = serializer;
        this.compressor = new CommonsCompressor(CompressorStreamFactory.GZIP);
    }

    public CompressorSerializer(ISerializer<Object> serializer, int compressionThreshold) {
        this.serializer = serializer;
        this.compressionThreshold = compressionThreshold;
        this.compressor = new CommonsCompressor(CompressorStreamFactory.GZIP);
    }

    public CompressorSerializer(ISerializer<Object> serializer, int compressionThreshold, String compressType) {
        this.serializer = serializer;
        this.compressionThreshold = compressionThreshold;
        this.compressor = new CommonsCompressor(compressType);
    }

    public CompressorSerializer(ISerializer<Object> serializer, int compressionThreshold, ICompressor compressor) {
        this.serializer = serializer;
        this.compressionThreshold = compressionThreshold;
        this.compressor = compressor;
    }

    @Override
    public byte[] serialize(final Object obj) throws Exception {
        if (null == obj) {
            return null;
        }
        byte[] data = serializer.serialize(obj);
        byte flag = 0;
        if (data.length > compressionThreshold) {
            data = compressor.compress(new ByteArrayInputStream(data));
            flag = 1;
        }
        byte[] out = new byte[data.length + 1];
        out[0] = flag;
        System.arraycopy(data, 0, out, 1, data.length);
        return out;
    }

    @Override
    public Object deserialize(final byte[] bytes, final Type returnType) throws Exception {
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        byte flag = bytes[0];
        byte[] data;
        if (flag == 0) {
            data = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, data, 0, data.length);
        } else {
            data = compressor.decompress(new ByteArrayInputStream(bytes, 1, bytes.length - 1));
        }
        return serializer.deserialize(data, returnType);
    }

    @Override
    public Object deepClone(Object obj, final Type type) throws Exception {
        return serializer.deepClone(obj, type);
    }

    @Override
    public Object[] deepCloneMethodArgs(Method method, Object[] args) throws Exception {
        return serializer.deepCloneMethodArgs(method, args);
    }
}
