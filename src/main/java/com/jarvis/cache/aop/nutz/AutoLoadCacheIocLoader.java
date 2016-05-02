package com.jarvis.cache.aop.nutz;

import org.nutz.ioc.loader.annotation.AnnotationIocLoader;

/**
 * @author Rekoe(koukou890@gmail.com)
 *
 */
public class AutoLoadCacheIocLoader extends AnnotationIocLoader {

    public AutoLoadCacheIocLoader() {
        super(AutoLoadCacheIocLoader.class.getPackage().getName());
    }

}
