package com.test;

/**
 * @author: jiayu.qiu
 */
public class Stopwatch {

    private long begin;

    private long end;

    private Stopwatch() {
    }

    public static Stopwatch begin() {
        Stopwatch sw=new Stopwatch();
        sw.begin=System.currentTimeMillis();
        return sw;
    }

    public void start() {
        this.begin=System.currentTimeMillis();
    }

    public void stop() {
        this.end=System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.valueOf(end - begin);
    }
}
