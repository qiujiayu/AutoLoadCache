package com.jarvis.cache.serializer.protobuf;

import java.io.OutputStream;

/**
 * @author zhengenshen@gmail.com
 */
public class ByteBufOutputStream extends OutputStream {

    private final WriteByteBuf buffer;

    public ByteBufOutputStream(WriteByteBuf buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        this.buffer = buffer;
    }

    @Override
    public void write(int b) {
        buffer.writeByte((byte)b);
    }

}
