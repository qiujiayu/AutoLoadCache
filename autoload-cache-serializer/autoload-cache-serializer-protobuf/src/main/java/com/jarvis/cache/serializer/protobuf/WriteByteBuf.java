package com.jarvis.cache.serializer.protobuf;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author zhengenshen@gmail.com
 */
@Slf4j
public class WriteByteBuf extends OutputStream {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private byte[] buf;

    private int count;

    public WriteByteBuf() {
        this(32);
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    public WriteByteBuf(int arrayLength) {
        buf = new byte[arrayLength];
    }

    public void writeByte(byte value) {
        int length = 1;
        ensureCapacity(length + count);
        HeapByteBufUtil.setByte(buf, count, value);
        count += length;
    }

    public void writeInt(int value) {
        int length = 4;
        ensureCapacity(length + count);
        HeapByteBufUtil.setInt(buf, count, value);
        count += length;
    }

    public void writeLong(long value) {
        int length = 8;
        ensureCapacity(length + count);
        HeapByteBufUtil.setLong(buf, count, value);
        count += length;
    }

    public void writeBytes(byte[] bytes) {
        int length = bytes.length;
        ensureCapacity(bytes.length + count);
        System.arraycopy(bytes, 0, buf, count, length);
        count += bytes.length;
    }


    public byte[] toByteArray() {
        byte[] newArray = new byte[count];
        System.arraycopy(buf, 0, newArray, 0, count);
        return newArray;
    }


    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0)
            grow(minCapacity);
    }


    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

}