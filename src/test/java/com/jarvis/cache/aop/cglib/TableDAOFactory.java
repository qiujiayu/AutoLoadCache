package com.jarvis.cache.aop.cglib;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;

public class TableDAOFactory {

    private static TableDao tDao=new TableDao();

    public static TableDao getInstance() {
        return tDao;
    }

    public static TableDao getAuthInstance(AuthProxy authProxy) {
        Enhancer en=new Enhancer(); // Enhancer用来生成一个原有类的子类
        // 进行代理
        en.setSuperclass(TableDao.class);
        // 设置织入逻辑
        // en.setCallback(authProxy);
        // en.setCallbacks()方法里的数组参数顺序就是上面方法的返回值所代表的方法拦截器，如果return 0则使用authProxy拦截器，
        // return 1则使用NoOp.INSTANCE拦截器，NoOp.INSTANCE是默认的方法拦截器，不做什么处理。
        en.setCallbacks(new Callback[]{authProxy, NoOp.INSTANCE}); // 设置两个方法拦截器
        en.setCallbackFilter(new AuthProxyFilter());
        // 生成代理实例
        return (TableDao)en.create();
    }
}