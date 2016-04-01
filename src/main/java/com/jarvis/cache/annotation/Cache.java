package com.jarvis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jarvis.cache.type.CacheOpType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface Cache {

    /**
     * 缓存的过期时间，单位：秒，如果为0则表示永久缓存
     * @return 时间
     */
    int expire();

    /**
     * 自定义缓存Key，支持Spring EL表达式
     * @return String 自定义缓存Key
     */
    String key();

    /**
     * 设置哈希表中的字段，如果设置此项，则用哈希表进行存储，支持Spring EL表达式
     * @return String
     */
    String hfield() default "";

    /**
     * 是否启用自动加载缓存， 缓存时间必须大于120秒时才有效
     * @return boolean
     */
    boolean autoload() default false;

    /**
     * 自动缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，如果设置了此值，autoload() 就失效，例如：null != #args[0].keyword，当第一个参数的keyword属性为null时设置为自动加载。
     * @return String SpEL表达式
     */
    String autoloadCondition() default "";

    /**
     * 当autoload为true时，缓存数据在 requestTimeout 秒之内没有使用了，就不进行自动加载数据,如果requestTimeout为0时，会一直自动加载
     * @return long 请求过期
     */
    long requestTimeout() default 36000L;

    /**
     * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存
     * @return String
     */
    String condition() default "";

    /**
     * 缓存的操作类型：默认是READ_WRITE，先缓存取数据，如果没有数据则从DAO中获取并写入缓存；如果是WRITE则从DAO取完数据后，写入缓存
     * @return CacheOpType
     */
    CacheOpType opType() default CacheOpType.READ_WRITE;

    /**
     * 并发等待时间(毫秒),等待正在DAO中加载数据的线程返回的等待时间。
     * @return 时间
     */
    int waitTimeOut() default 500;

    /**
     * 扩展缓存
     * @return
     */
    ExCache[] exCache() default @ExCache(expire=-1, key="");
}