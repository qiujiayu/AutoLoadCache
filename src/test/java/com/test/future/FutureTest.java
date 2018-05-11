package com.test.future;

import java.util.concurrent.CompletableFuture;

public class FutureTest {

    public static void main(String[] args) {
        test1();
        System.out.println(222);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void test2() {
        CompletableFuture<Double> futurePrice = getPriceAsync();
        //do anything you want, 当前线程不被阻塞
        System.out.println("test2");
        //线程任务完成的话，执行回调函数，不阻塞后续操作
        futurePrice.whenCompleteAsync((aDouble, throwable) -> {
            System.out.println(aDouble+":"+Thread.currentThread().getName());
            //do something else
        });
    }
    
    private static void test1() {
        CompletableFuture<Double> futurePrice = getPriceAsync();
        //do anything you want, 当前线程不被阻塞
        System.out.println(111);
        //线程任务完成的话，执行回调函数，不阻塞后续操作
        futurePrice.whenComplete((aDouble, throwable) -> {
            System.out.println(aDouble+":"+Thread.currentThread().getName());
            //do something else
        });
    }

    static CompletableFuture<Double> getPriceAsync() {
        CompletableFuture<Double> futurePrice = new CompletableFuture<>();
        new Thread(() -> {
            System.out.println("Thread:"+Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            futurePrice.complete(23.55);
        }).start();
        return futurePrice;
    }
}
