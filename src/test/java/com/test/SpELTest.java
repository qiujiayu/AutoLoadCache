package com.test;

import com.jarvis.cache.CacheUtil;

public class SpELTest {

    public static void main(String[] args) {
        String keySpEL="test";
        Object[] arguments=new Object[]{"1111", "2222"};

        String res=CacheUtil.getDefinedCacheKey(keySpEL, arguments, null, false);
        System.out.println(res);
        Boolean rv=CacheUtil.getElValue("#empty(#args)", arguments, Boolean.class);
        System.out.println(rv);
    }

}
