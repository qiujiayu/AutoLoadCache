package com.test;

public class Simple implements java.io.Serializable {

    private static final long serialVersionUID=-4333266259384775868L;

    private String name;

    private Integer age;

    private String sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age=age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex=sex;
    }

    static Simple getSimple() {
        Simple simple=new Simple();
        simple.setAge(10);
        simple.setName("XiaoMing");
        simple.setSex("2");
        return simple;
    }

    public String toString() {
        return "name=" + name + ";age=" + age + ";sex=" + sex;
    }
}