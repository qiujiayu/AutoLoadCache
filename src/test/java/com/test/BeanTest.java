package com.test;

import java.util.Calendar;

import com.jarvis.lib.util.BeanUtil;

public class BeanTest {

    public static void main(String args[]) {
        Calendar now=Calendar.getInstance();
        String str=BeanUtil.toString(now.getTime().getTime());
        System.out.println(now.toString());
        System.out.println(String.valueOf(now));
        System.out.println(str);


    }
}
