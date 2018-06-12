package com.jarvis.cache.aop.aspectj;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import com.jarvis.cache.aop.DeleteCacheAopProxyChain;

/**
 * @author: jiayu.qiu
 */
public class AspectjDeleteCacheAopProxyChain implements DeleteCacheAopProxyChain {

    private JoinPoint jp;

    public AspectjDeleteCacheAopProxyChain(JoinPoint jp) {
        this.jp = jp;
    }

    @Override
    public Object[] getArgs() {
        return jp.getArgs();
    }

    @Override
    public Object getTarget() {
        return jp.getTarget();
    }

    @Override
    public Method getMethod() {
        Signature signature = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getMethod();
    }

}
