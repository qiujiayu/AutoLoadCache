package com.jarvis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通过分割参数或返回值生成批量删除缓存Key注解
 *
 * @author jiayu.qiu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface CacheDeleteMagicKey {

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
    String value();

    /**
     * 哈希表中的字段表达式
     *
     * @return String
     */
    String hfield() default "";

    /**
     * 需要分割处理的参数索引，如果不需要分割，则将参数设置为-1
     * @return int
     */
    int iterableArgIndex();

    /**
     * 是否分割返回值
     * @return
     */
    boolean iterableReturnValue() default false;
}
