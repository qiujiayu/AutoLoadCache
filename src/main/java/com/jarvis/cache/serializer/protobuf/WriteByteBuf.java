package com.jarvis.cache.serializer.protobuf;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author zhengenshen@gmail.com
 */
@Slf4j
public class WriteByteBuf {

    private static final int DEFAULT_ARRAY_LENGTH = 1024 * 100;

    private byte[] array;

    /**
     * 索引下标
     */
    private int readerIndex;

    private int writerIndex;

    /**
     * 已写入数据尾标
     */
    private int limit;

    /**
     * array 长度
     */
    private int arrayLength;

    public WriteByteBuf() {
        this(DEFAULT_ARRAY_LENGTH);
    }

    public WriteByteBuf(int arrayLength) {
        array = new byte[arrayLength];
        this.readerIndex = this.writerIndex = 0;
        this.arrayLength = arrayLength;
    }


    public void writeByte(byte value) {
        int length = 1;
        hasLength(length);
        HeapByteBufUtil.setByte(array, writerIndex, value);
        writerIndex += length;
        limit = writerIndex;
    }

    public void writeInt(int value) {
        int length = 4;
        hasLength(length);
        HeapByteBufUtil.setInt(array, writerIndex, value);
        writerIndex += length;
        limit = writerIndex;
    }


    public void writeLong(long value) {
        int length = 8;
        hasLength(length);
        HeapByteBufUtil.setLong(array, writerIndex, value);
        writerIndex += length;
        limit = writerIndex;
    }


    /**
     * 可读字节
     */
    public byte[] readableBytes() {
        byte[] newArray = new byte[limit - readerIndex];
        System.arraycopy(array, readerIndex, newArray, 0, newArray.length);
        return newArray;
    }

    public void resetIndex() {
        writerIndex = readerIndex = limit = 0;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    private void hasLength(int length) {
        if (limit + length < arrayLength) {
            return;
        }
        //TODO 扩容
        log.error("current byte stream length {} write length {}", limit, length);
        throw new ArrayIndexOutOfBoundsException();
    }

    public void writeBytes(byte[] bytes) {
        hasLength(bytes.length);
        System.arraycopy(bytes, 0, array, writerIndex, bytes.length);
        this.limit += bytes.length;
    }

    public void writeObject(Object obj) {
        if (obj == null) {
            return;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(outputStream);
            output.writeObject(obj);
            output.flush();
            writeBytes(outputStream.toByteArray());
        } catch (IOException e) {
            log.warn("byteBuf writeObject error");
        }

    }
}
