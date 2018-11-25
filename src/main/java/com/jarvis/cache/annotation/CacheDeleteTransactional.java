package com.jarvis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事务环境中批量删除缓存注解<br>
 * 注意：此注解放到service层，并且需要开启事务的方法上, 用于收集@CacheDeleteKey生成的Key,并在最后进行删除缓存。
 * 
 * @author jiayu.qiu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface CacheDeleteTransactional {
    /**
     * 在事务环境中是否使用缓存数据，默认为false
     * 
     * @return true or false
     */
    boolean useCache() default false;

    /**
     * 当发生异常时，还删除缓存
     * @return true or false
     */
    boolean deleteCacheOnError() default true;
}
