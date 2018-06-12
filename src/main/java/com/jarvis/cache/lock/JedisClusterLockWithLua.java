package com.jarvis.cache.lock;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.JedisCluster;

/**
 * @author: jiayu.qiu
 */
public class JedisClusterLockWithLua extends AbstractRedisLockWithLua {

    private JedisCluster jedisCluster;

    public JedisClusterLockWithLua(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    protected Long eval(byte[] lockScript, String key, List<byte[]> args) throws UnsupportedEncodingException {
        List<byte[]> keys = new ArrayList<byte[]>();
        keys.add(key.getBytes("UTF-8"));
        return (Long) jedisCluster.eval(lockScript, keys, args);
    }

    @Override
    protected void del(String key) {
        jedisCluster.del(key);
    }

}
