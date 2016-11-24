package com.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

public class BeanTest {

    private static final ConcurrentHashMap<String, Boolean> processing=new ConcurrentHashMap<String, Boolean>();

    @Test
    public void testputIfAbsent() {
        Boolean isProcessing=processing.putIfAbsent("k1", Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
        assertNull(isProcessing);
        System.out.println("isProcessing1==" + isProcessing);
        isProcessing=processing.putIfAbsent("k1", Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
        System.out.println("isProcessing2==" + isProcessing);
        assertEquals(isProcessing, Boolean.TRUE);

    }

}
