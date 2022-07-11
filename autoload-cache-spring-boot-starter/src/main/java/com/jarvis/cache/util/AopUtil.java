package com.jarvis.cache.util;

import org.springframework.aop.framework.AopProxyUtils;

/**
 *
 */
public class AopUtil {

    /**
     * @param target
     * @return
     */
    public static Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null && target != null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }

}
