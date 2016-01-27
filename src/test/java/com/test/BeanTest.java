package com.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanTest {
    private static final ConcurrentHashMap<String, Boolean> processing=new ConcurrentHashMap<String, Boolean>();

    public static void main(String args[]) {
        Boolean isProcessing=processing.putIfAbsent("k1", Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
        System.out.println("isProcessing1=="+isProcessing);
        isProcessing=processing.putIfAbsent("k1", Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
        System.out.println("isProcessing2=="+isProcessing);
        
        
    }

   
}
