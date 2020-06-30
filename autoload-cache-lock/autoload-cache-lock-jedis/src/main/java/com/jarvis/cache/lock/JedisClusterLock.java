package com.jarvis.cache.lock;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

/**
 *
 */
public class JedisClusterLock extends AbstractRedisLock {

    private JedisCluster jedisCluster;

    public JedisClusterLock(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    protected boolean setnx(String key, String val, int expire) {
        return OK.equalsIgnoreCase(jedisCluster.set(key, val, SetParams.setParams().nx().ex(expire)));
    }

    @Override
    protected void del(String key) {
        this.jedisCluster.del(key);
    }

}
