package com.jarvis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cache {

    /**
     * 缓存的过期时间，单位：秒
     */
    int expire();

    /**
     * 是否启用自动加载缓存
     * @return
     */
    boolean autoload() default false;

    /**
     * 当autoload为true时，缓存数据在 requestTimeout 秒之内没有使用了，就不进行自动加载数据,如果requestTimeout为0时，会一直自动加载
     * @return
     */
    long requestTimeout() default 36000L;
    
    /**
     * 使用SpEL，将缓存key，根据业务需要进行二次分组
     * @return
     */
    String subKeySpEL() default "";
    /**
     * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存
     * @return
     */
    String condition() default "";
}