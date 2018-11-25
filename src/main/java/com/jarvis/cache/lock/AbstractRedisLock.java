package com.jarvis.cache.lock;

import java.util.HashMap;
import java.util.Map;

import com.jarvis.cache.to.RedisLockInfo;

/**
 * 基于Redis实现分布式锁
 * 
 * @author jiayu.qiu
 */
public abstract class AbstractRedisLock implements ILock {

    private static final ThreadLocal<Map<String, RedisLockInfo>> LOCK_START_TIME = new ThreadLocal<Map<String, RedisLockInfo>>() {
        @Override
        protected Map<String, RedisLockInfo> initialValue() {
            return new HashMap<String, RedisLockInfo>(4);
        }
    };

    /**
     * SETNX
     * 
     * @param key key
     * @param val vale
     * @return 是否设置成功
     */
    protected abstract Boolean setnx(String key, String val);

    /**
     * EXPIRE
     * 
     * @param key key
     * @param expire 过期时间
     */
    protected abstract void expire(String key, int expire);

    /**
     * GET
     * 
     * @param key key
     * @return 缓存数据
     */
    protected abstract String get(String key);

    /**
     * GETSET
     * 
     * @param key key
     * @param newVal new value
     * @return redis 中的老数据
     */
    protected abstract String getSet(String key, String newVal);

    private long serverTimeMillis() {
        return System.currentTimeMillis();
    }

    private boolean isTimeExpired(String value) {
        return serverTimeMillis() > Long.parseLong(value);
    }

    /**
     * DEL
     * 
     * @param key key
     */
    protected abstract void del(String key);

    @Override
    public boolean tryLock(String key, int lockExpire) {
        boolean locked = getLock(key, lockExpire);

        if (locked) {
            Map<String, RedisLockInfo> startTimeMap = LOCK_START_TIME.get();
            RedisLockInfo info = new RedisLockInfo();
            info.setLeaseTime(lockExpire * 1000);
            info.setStartTime(System.currentTimeMillis());
            startTimeMap.put(key, info);
        }
        return locked;
    }

    private boolean getLock(String key, int lockExpire) {
        long lockExpireTime = serverTimeMillis() + (lockExpire * 1000) + 1;// 锁超时时间
        String lockExpireTimeStr = String.valueOf(lockExpireTime);
        if (setnx(key, lockExpireTimeStr)) {// 获取到锁
            try {
                expire(key, lockExpire);
            } catch (Throwable e) {
            }
            return true;
        }
        String oldValue = get(key);
        if (oldValue != null && isTimeExpired(oldValue)) { // lock is expired
            String oldValue2 = getSet(key, lockExpireTimeStr); // getset is
                                                               // atomic
            // 但是走到这里时每个线程拿到的oldValue肯定不可能一样(因为getset是原子性的)
            // 假如拿到的oldValue依然是expired的，那么就说明拿到锁了
            if (oldValue2 != null && isTimeExpired(oldValue2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void unlock(String key) {
        Map<String, RedisLockInfo> startTimeMap = LOCK_START_TIME.get();
        RedisLockInfo info = null;
        if (null != startTimeMap) {
            info = startTimeMap.remove(key);
        }
        // 如果超过租约时间则不需要主到释放锁
        if (null != info && (System.currentTimeMillis() - info.getStartTime()) >= info.getLeaseTime()) {
            return;
        }
        try {
            del(key);
        } catch (Throwable e) {
        }
    }
}
