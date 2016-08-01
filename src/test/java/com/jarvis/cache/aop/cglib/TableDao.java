package com.jarvis.cache.aop.cglib;

public class TableDao {

    public void create() {
        System.out.println("create() is running...");
    }

    public void delete() {
        System.out.println("delete() is running...");
    }

    public void update() {
        System.out.println("update() is running...");
    }

    public void query() {
        System.out.println("query() is running...");
    }
}