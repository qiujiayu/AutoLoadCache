package com.jarvis.cache.clone;

import com.rits.cloning.Cloner;

public class Cloning implements ICloner {

    private final Cloner cloner=new Cloner();

    @Override
    public Object deepClone(Object obj) throws Exception {
        if(null == obj) {
            return null;
        }
        return cloner.deepClone(obj);
    }

}
