package com.jarvis.cache.lock;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * @author: jiayu.qiu
 */
public class ShardedJedisLockWithLua extends AbstractRedisLockWithLua {

    private static final Logger logger = LoggerFactory.getLogger(ShardedJedisLockWithLua.class);

    private ShardedJedisPool shardedJedisPool;

    public ShardedJedisLockWithLua(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    private void returnResource(ShardedJedis shardedJedis) {
        shardedJedis.close();
    }

    @Override
    protected Long eval(byte[] lockScript, String key, List<byte[]> args) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            List<byte[]> keys = new ArrayList<byte[]>();
            keys.add(key.getBytes("UTF-8"));
            return (Long) jedis.eval(lockScript, keys, args);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            returnResource(shardedJedis);
        }
        return 0L;
    }

    @Override
    protected void del(String key) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            jedis.del(key);
        } finally {
            returnResource(shardedJedis);
        }
    }

}
