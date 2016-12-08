package com.jarvis.cache;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * @author jiayu.qiu
 */
public class DataLoaderFactory extends BasePooledObjectFactory<DataLoader> {

    private static DataLoaderFactory instance;

    private final GenericObjectPool<DataLoader> factory;

    private DataLoaderFactory() {
        GenericObjectPoolConfig config=new GenericObjectPoolConfig();
        config.setMaxTotal(1024);
        config.setMaxIdle(50);
        config.setMinIdle(8);
        config.setBlockWhenExhausted(false);// 当Pool中没有对象时不等待，而是直接new个新的
        factory=new GenericObjectPool<DataLoader>(this, config);
    }

    public static DataLoaderFactory getInstance() {
        if(null == instance) {
            synchronized(DataLoaderFactory.class) {
                if(null == instance) {
                    instance=new DataLoaderFactory();
                }
            }
        }
        return instance;
    }

    public DataLoader getDataLoader() {
        try {
            return factory.borrowObject();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new DataLoader();
    }

    public void returnObject(DataLoader loader) {
        factory.returnObject(loader);
    }

    @Override
    public DataLoader create() throws Exception {
        return new DataLoader();
    }

    @Override
    public PooledObject<DataLoader> wrap(DataLoader obj) {
        return new DefaultPooledObject<DataLoader>(obj);
    }
}
