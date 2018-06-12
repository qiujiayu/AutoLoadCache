package com.jarvis.cache.lock;

/**
 * 分布式锁接口
 * 
 * @author jiayu.qiu
 */
public interface ILock {

    /**
     * 获取分布式锁
     * 
     * @param key 锁Key
     * @param lockExpire 锁的缓存时间（单位：秒）
     * @return boolean
     */
    boolean tryLock(String key, int lockExpire);

    /**
     * 释放锁
     * 
     * @param key 锁Key
     */
    void unlock(String key);
}
