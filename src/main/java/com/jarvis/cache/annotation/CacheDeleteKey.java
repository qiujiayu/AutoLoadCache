package com.jarvis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 生成删除缓存Key注解
 * 
 * @author jiayu.qiu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface CacheDeleteKey {

    /**
     * 缓存的条件表达式，可以为空，返回 true 或者 false，只有为 true 才进行缓存
     * 
     * @return String
     */
    String condition() default "";

    /**
     * 删除缓存的Key表达式, 当value有值时，是自定义缓存key（删除缓存不支持默认缓存key）。
     * 
     * @return String
     */
    String[] value();

    /**
     * 哈希表中的字段表达式
     * 
     * @return String
     */
    String hfield() default "";
}
