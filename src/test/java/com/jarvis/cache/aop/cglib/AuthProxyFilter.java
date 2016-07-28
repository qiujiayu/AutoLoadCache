package com.jarvis.cache.aop.cglib;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.CallbackFilter;

public class AuthProxyFilter implements CallbackFilter {

    public int accept(Method arg0) {
        /*
         * 如果调用的不是query方法，则要调用authProxy拦截器去判断权限
         */
        if(!"query".equalsIgnoreCase(arg0.getName())){
            return 0; //调用第一个方法拦截器，即authProxy
        }
        /*
         * 调用第二个方法拦截器，即NoOp.INSTANCE，NoOp.INSTANCE是指不做任何事情的拦截器
         * 在这里就是任何人都有权限访问query方法，所以调用默认拦截器不做任何处理
         */
        return 1;  
    }

}
