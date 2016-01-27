package com.jarvis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jarvis.cache.type.CacheKeyType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheDeleteKey {

    /**
     * 缓存的条件，可以为空，使用 SpEL 编写，返回 true 或者 false，只有为 true 才进行缓存
     * @return String
     */
    String condition() default "";

    /**
     * 删除缓存的Key，支持使用SpEL表达式, 当value有值时，是自定义缓存key，否则按默认缓存key处理。
     * @return String
     */
    String value() default "";

    /**
     * 生成Key的类型
     * @return CacheKeyType
     */
    @Deprecated
    CacheKeyType keyType();

    /**
     * deleteByPrefixKey 是否根据前缀进行批量删除 只有当 keType为DEFAULT时才有效
     * @return boolean
     */
    boolean deleteByPrefixKey() default false;

    /**
     * 缓存的Class 只有当 keType为DEFAULT时才有效
     * @return Class
     */
    @SuppressWarnings("rawtypes")
    Class cls() default Class.class;

    /**
     * 缓存所在的方法名
     * @return String
     */
    String method() default "";

    /**
     * 转换缓存方法的参数，当keyType 为DEFAULT 并且 deleteByPrefixKey=false时才会使用上。
     * @return String[]
     */
    String[] argsEl() default "";

    /**
     * 使用SpEL，将缓存key，根据业务需要进行二次分组（使用默认缓存Key的时候才有效） 只有当 keType为DEFAULT时才有效
     * @return String
     */
    String subKeySpEL() default "";

}
