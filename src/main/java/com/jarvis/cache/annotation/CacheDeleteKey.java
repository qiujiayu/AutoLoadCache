package com.jarvis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheDeleteKey {

    /**
     * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存
     * @return String
     */
    String condition() default "";

    /**
     * 删除缓存的Key，支持使用SpEL表达式, 当value有值时，是自定义缓存key（删除缓存不支持默认缓存key）。
     * @return String
     */
    String value();

    /**
     * 哈希表中的字段，支持使用SpEL表达式
     * @return String
     */
    String hfield() default "";
}
