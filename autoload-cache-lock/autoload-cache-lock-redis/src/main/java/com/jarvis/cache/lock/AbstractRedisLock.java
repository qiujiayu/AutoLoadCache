package com.jarvis.cache.lock;

import com.jarvis.cache.to.RedisLockInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于Redis实现分布式锁
 *
 *
 */
@Slf4j
public abstract class AbstractRedisLock implements ILock {

    private static final ThreadLocal<Map<String, RedisLockInfo>> LOCK_START_TIME = new ThreadLocal<Map<String, RedisLockInfo>>() {
        @Override
        protected Map<String, RedisLockInfo> initialValue() {
            return new HashMap<>(4);
        }
    };

    protected static final String OK = "OK";

    protected static final String NX = "NX";

    protected static final String EX = "EX";

    /**
     * SETNX
     *
     * @param key    key
     * @param val    vale
     * @param expire 过期时间
     * @return 是否设置成功
     */
    protected abstract boolean setnx(String key, String val, int expire);

    /**
     * DEL
     *
     * @param key key
     */
    protected abstract void del(String key);

    @Override
    public boolean tryLock(String key, int lockExpire) {
        boolean locked = setnx(key, OK, lockExpire);
        if (locked) {
            Map<String, RedisLockInfo> startTimeMap = LOCK_START_TIME.get();
            RedisLockInfo info = new RedisLockInfo();
            info.setLeaseTime(lockExpire * 1000);
            info.setStartTime(System.currentTimeMillis());
            startTimeMap.put(key, info);
        }
        return locked;
    }

    @Override
    public void unlock(String key) {
        Map<String, RedisLockInfo> startTimeMap = LOCK_START_TIME.get();
        RedisLockInfo info = null;
        if (null != startTimeMap) {
            info = startTimeMap.remove(key);
        }
        // 如果实际执行时长超过租约时间则不需要主到释放锁
        long useTime = System.currentTimeMillis() - info.getStartTime();
        if (null != info && useTime >= info.getLeaseTime()) {
            log.warn("lock(" + key + ") run timeout, use time:" + useTime);
            return;
        }
        try {
            del(key);
        } catch (Throwable e) {
        }
    }
}
