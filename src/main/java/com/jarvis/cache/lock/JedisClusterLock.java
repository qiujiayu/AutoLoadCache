package com.jarvis.cache.lock;

import redis.clients.jedis.JedisCluster;

/**
 * @author: jiayu.qiu
 */
public class JedisClusterLock extends AbstractRedisLock {

    private JedisCluster jedisCluster;

    public JedisClusterLock(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    protected Boolean setnx(String key, String val) {
        return this.jedisCluster.setnx(key, val).intValue() == 1;
    }

    @Override
    protected void expire(String key, int expire) {
        this.jedisCluster.expire(key, expire);
    }

    @Override
    protected String get(String key) {
        return this.jedisCluster.get(key);
    }

    @Override
    protected String getSet(String key, String newVal) {
        return this.jedisCluster.getSet(key, newVal);
    }

    @Override
    protected void del(String key) {
        this.jedisCluster.del(key);
    }

}
