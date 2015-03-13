package com.jarvis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheDelete {

    /**
     * 删除缓存的Key，支持使用SpEL表达式
     * @return
     */
    String[] value();
}
