package com.jarvis.cache.serializer.protobuf;

/**
 * @author zhengenshen@gmail.com
 */
public class ReadByteBuf {

    private byte[] array;
    private int readerIndex;


    public ReadByteBuf setBytes(byte[] bytes) {
        this.array = bytes;
        this.readerIndex = 0;
        return this;
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
