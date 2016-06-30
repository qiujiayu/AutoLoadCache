package com.jarvis.cache.clone;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.rits.cloning.Cloner;

public class Cloning implements ICloner {

    private final Cloner cloner=new Cloner();

    @Override
    public Object deepClone(Object obj, final Type type) throws Exception {
        if(null == obj) {
            return null;
        }
        return cloner.deepClone(obj);
    }

    @Override
    public Object[] deepCloneMethodArgs(Method method, Object[] args) throws Exception {
        return (Object[])deepClone(args, null);
    }

}
