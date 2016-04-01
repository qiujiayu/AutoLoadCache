package com.jarvis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
/**
 * 对@Cache进行扩展，实现一次请求生成多个缓存数，减少与DAO的交互次数
 * @author jiayu.qiu
 *
 */
public @interface ExCache {

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
     * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存
     * @return String
     */
    String condition() default "";

    /**
     * 通过SpringEL表达式获取需要缓存的数据，如果没有设置，则默认使用 #retVal
     * @return
     */
    String cacheObject() default "";

}
