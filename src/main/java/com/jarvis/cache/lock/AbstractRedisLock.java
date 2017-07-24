package com.jarvis.cache.lock;

import java.util.HashMap;
import java.util.Map;

import com.jarvis.cache.to.RedisLockInfo;

/**
 * 基于Redis实现分布式锁
 * @author jiayu.qiu
 */
public abstract class AbstractRedisLock implements ILock {

    private static final ThreadLocal<Map<String, RedisLockInfo>> LOCK_START_TIME=new ThreadLocal<Map<String, RedisLockInfo>>();

    protected abstract Long setnx(String key, String val);

    protected abstract void expire(String key, int expire);

    protected abstract String get(String key);

    protected abstract String getSet(String key, String newVal);

    private long serverTimeMillis() {
        return System.currentTimeMillis();
    }

    private boolean isTimeExpired(String value) {
        return serverTimeMillis() > Long.parseLong(value);
    }

    protected abstract void del(String key);

    @Override
    public boolean tryLock(String key, int lockExpire) {
        boolean locked=getLock(key, lockExpire);

        if(locked) {
            Map<String, RedisLockInfo> startTimeMap=LOCK_START_TIME.get();
            if(null == startTimeMap) {
                startTimeMap=new HashMap<String, RedisLockInfo>();
                LOCK_START_TIME.set(startTimeMap);
            }
            RedisLockInfo info=new RedisLockInfo();
            info.setLeaseTime(lockExpire * 1000);
            info.setStartTime(System.currentTimeMillis());
            startTimeMap.put(key, info);
        }
        return locked;
    }

    private boolean getLock(String key, int lockExpire) {
        long lockExpireTime=serverTimeMillis() + (lockExpire * 1000) + 1;// 锁超时时间
        String lockExpireTimeStr=String.valueOf(lockExpireTime);
        if(setnx(key, lockExpireTimeStr).intValue() == 1) {// 获取到锁
            try {
                expire(key, lockExpire);
            } catch(Throwable e) {
            }
            return true;
        }
        String oldValue=get(key);
        if(oldValue != null && isTimeExpired(oldValue)) { // lock is expired
            String oldValue2=getSet(key, lockExpireTimeStr); // getset is atomic
            // 但是走到这里时每个线程拿到的oldValue肯定不可能一样(因为getset是原子性的)
            // 假如拿到的oldValue依然是expired的，那么就说明拿到锁了
            if(oldValue2 != null && isTimeExpired(oldValue2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void unlock(String key) {
        Map<String, RedisLockInfo> startTimeMap=LOCK_START_TIME.get();
        RedisLockInfo info=null;
        if(null != startTimeMap) {
            info=startTimeMap.remove(key);
        }
        if(null != info && (System.currentTimeMillis() - info.getStartTime()) >= info.getLeaseTime()) {
            return;
        }
        try {
            del(key);
        } catch(Throwable e) {
        }
    }
}
