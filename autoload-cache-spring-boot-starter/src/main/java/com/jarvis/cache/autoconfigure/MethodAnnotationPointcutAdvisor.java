package com.jarvis.cache.autoconfigure;

import java.lang.annotation.Annotation;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

/**
 * PointcutAdvisor
 * 
 * @author jiayu.qiu
 */
public class MethodAnnotationPointcutAdvisor extends AbstractPointcutAdvisor {

    private static final long serialVersionUID = -7431399745989513294L;

    private final Pointcut pointcut;

    private final Advice advice;

    public MethodAnnotationPointcutAdvisor(Class<? extends Annotation> methodAnnotationType, Advice advice) {
        this.pointcut = new AnnotationMatchingPointcut(null, methodAnnotationType);
        this.advice = advice;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

}
