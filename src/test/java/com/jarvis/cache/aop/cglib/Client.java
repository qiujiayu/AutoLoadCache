package com.jarvis.cache.aop.cglib;


public class Client {

    public static void main(String[] args) {  
        haveAuth(); 
        haveNoAuth();
    }  
    public static void doMethod(TableDao dao){  
        dao.create();  
        dao.query();  
        dao.update();  
        dao.delete();  
    }  
    //模拟有权限
    public static void haveAuth(){  
        TableDao tDao = TableDAOFactory.getAuthInstance(new AuthProxy("张三"));  
        doMethod(tDao);  
    }  
    //模拟无权限
    public static void haveNoAuth(){  
        TableDao tDao = TableDAOFactory.getAuthInstance(new AuthProxy("李四"));  
        doMethod(tDao);  
    }
}
