package com.jarvis.cache.serializer.protobuf;

/**
 * @author zhengenshen@gmail.com
 */
public class ReadByteBuf {

    private byte[] array;
    private int readerIndex;

    public ReadByteBuf(byte[] array) {
        this.array = array;
        this.readerIndex = 0;
    }

    public byte readByte() {
        byte value = HeapByteBufUtil.getByte(array, readerIndex);
        readerIndex += 1;
        return value;
    }

    public int readInt() {
        int value = HeapByteBufUtil.getInt(array, readerIndex);
        readerIndex += 4;
        return value;
    }

    public long readLong() {
        long value = HeapByteBufUtil.getLong(array, readerIndex);
        readerIndex += 8;
        return value;
    }

    public byte[] readableBytes() {
        byte[] newArray = new byte[array.length - readerIndex];
        System.arraycopy(array, readerIndex, newArray, 0, newArray.length);
        return newArray;
    }

}
