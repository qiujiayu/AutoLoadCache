package com.jarvis.cache.annotation;

        import java.lang.annotation.Documented;
        import java.lang.annotation.ElementType;
        import java.lang.annotation.Inherited;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

/**
 * 魔术模式：
 * 为了降低缓存数据不一致问题，通常会将数据根据id进行缓存，更新时只需要更新此缓存数据； <br>
 * 比如：User id 为1的数据，缓存key为：user_1；<br>
 * 分隔参数后，批量去缓存中数据，没有命中的再批量到数据源获取数据;<br>
 *
 *
 * @author jiayu.qiu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface Magic {

    /**
     * 通过返回值缓存Key，用于与通过参数生的缓存key进行匹配
     *
     * @return String 自定义缓存Key
     */
    String key();

    /**
     * 设置哈希表中的字段，如果设置此项，则用哈希表进行存储，支持表达式
     *
     * @return String
     */
    String hfield() default "";

    /**
     * 需要分隔处理的参数索引
     * @return int
     */
    int iterableArgIndex() default 0;
}