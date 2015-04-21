package com.jarvis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jarvis.cache.type.CacheOpType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cache {

    /**
     * 缓存的过期时间，单位：秒
     */
    int expire();

    /**
     * 自定义缓存Key,如果不设置使用系统默认生成缓存Key的方法
     * @return
     */
    String key() default "";

    /**
     * 是否启用自动加载缓存
     * @return
     */
    boolean autoload() default false;

    /**
     * 自动缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，如果设置了此值，autoload() 就失效，例如：null != #args[0].keyword，当第一个参数的keyword属性为null时设置为自动加载。
     * @return
     */
    String autoloadCondition() default "";

    /**
     * 当autoload为true时，缓存数据在 requestTimeout 秒之内没有使用了，就不进行自动加载数据,如果requestTimeout为0时，会一直自动加载
     * @return
     */
    long requestTimeout() default 36000L;

    /**
     * 使用SpEL，将缓存key，根据业务需要进行二次分组（使用默认缓存Key的时候才有效）
     * @return
     */
    String subKeySpEL() default "";

    /**
     * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存
     * @return
     */
    String condition() default "";

    /**
     * 缓存的操作类型：默认是READ_WRITE，先缓存取数据，如果没有数据则从DAO中获取并写入缓存；如果是WRITE则从DAO取完数据后，写入缓存
     * @return
     */
    CacheOpType opType() default CacheOpType.READ_WRITE;
}