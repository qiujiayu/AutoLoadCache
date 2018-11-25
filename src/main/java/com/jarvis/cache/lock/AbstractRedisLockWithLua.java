package com.jarvis.cache.lock;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jarvis.cache.to.RedisLockInfo;

/**
 * 基于Redis+Lua实现分布式锁; 实现方法更容易理解，但性能相对会差些
 * 
 * @author jiayu.qiu
 */
public abstract class AbstractRedisLockWithLua implements ILock {

    private static final ThreadLocal<Map<String, RedisLockInfo>> LOCK_START_TIME = new ThreadLocal<Map<String, RedisLockInfo>>();

    /**
     * 分布式锁 KEY[1] lock key <br>
     * ARGV[1] 过期时间 ARGV[2] 缓存时长 返回值: 如果执行成功, 则返回1; 否则返回0
     */
    private static final String LOCK_SCRIPT_STR = "local lockKey= KEYS[1]\n"//
            + "local lock = redis.call('SETNX', lockKey, ARGV[1])\n" // 持锁
            + "if lock == 0 then\n" //
            + "  return 0\n" //
            + "end\n" //
            + "redis.call('EXPIRE', lockKey, tonumber(ARGV[2]))\n" // 持锁n秒
            + "return 1\n";

    private static byte[] lockScript;

    static {
        try {
            lockScript = LOCK_SCRIPT_STR.getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * eval
     * 
     * @param lockScript lua 脚本
     * @param key key
     * @param args 参数
     * @return 是否获取锁成功，1成功，否则失败
     * @throws UnsupportedEncodingException 编码异常
     */
    protected abstract Long eval(byte[] lockScript, String key, List<byte[]> args) throws UnsupportedEncodingException;

    /**
     * del
     * 
     * @param key key
     */
    protected abstract void del(String key);

    @Override
    public boolean tryLock(String key, int lockExpire) {
        boolean locked = getLock(key, lockExpire);

        if (locked) {
            Map<String, RedisLockInfo> startTimeMap = LOCK_START_TIME.get();
            if (null == startTimeMap) {
                startTimeMap = new HashMap<String, RedisLockInfo>(8);
                LOCK_START_TIME.set(startTimeMap);
            }
            RedisLockInfo info = new RedisLockInfo();
            info.setLeaseTime(lockExpire * 1000);
            info.setStartTime(System.currentTimeMillis());
            startTimeMap.put(key, info);
        }
        return locked;

    }

    private boolean getLock(String key, int lockExpire) {
        try {
            List<byte[]> args = new ArrayList<byte[]>();
            long expire2 = System.currentTimeMillis() + (lockExpire * 1000);
            args.add(String.valueOf(expire2).getBytes("UTF-8"));
            args.add(String.valueOf(lockExpire).getBytes("UTF-8"));

            Long rv = eval(lockScript, key, args);
            return null != rv && rv.intValue() == 1;
        } catch (Exception e) {
            e.printStackTrace();
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
        if (null != info && (System.currentTimeMillis() - info.getStartTime()) >= info.getLeaseTime()) {
            return;
        }
        try {
            del(key);
        } catch (Throwable e) {
        }
    }
}
