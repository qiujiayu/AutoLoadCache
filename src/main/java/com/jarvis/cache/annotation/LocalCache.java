package com.jarvis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 本地缓存注解
 * 
 * @author jiayu.qiu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface LocalCache {

    /**
     * 缓存的过期时间，单位：秒，如果为0则表示永久缓存
     * 
     * @return 时间
     */
    int expire();

    /**
     * 动态获取缓存过期时间的表达式
     * 
     * @return 时间表达式
     */
    String expireExpression() default "";

    /**
     * 只缓存在本地
     * 
     * @return boolean
     */
    boolean localOnly() default false;
}