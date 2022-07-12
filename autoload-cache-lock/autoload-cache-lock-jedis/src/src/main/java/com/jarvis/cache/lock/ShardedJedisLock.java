package com.jarvis.cache.lock;

import redis.clients.jedis.JedisSharding;
import redis.clients.jedis.params.SetParams;

/**
 *
 */
public class ShardedJedisLock extends AbstractRedisLock {

    private JedisSharding jedisSharding;

    public ShardedJedisLock(JedisSharding jedisSharding) {
        this.jedisSharding = jedisSharding;
    }

    private void returnResource(JedisSharding jedisSharding) {
        jedisSharding.close();
    }

    @Override
    protected boolean setnx(String key, String val, int expire) {
        /*ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            return OK.equalsIgnoreCase(jedis.set(key, val, SetParams.setParams().nx().ex(expire)));
        } finally {
            returnResource(shardedJedis);
        }*/
        try {
            return OK.equalsIgnoreCase(jedisSharding.set(key, val, SetParams.setParams().nx().ex(expire)));
        } finally {
            returnResource(jedisSharding);
        }
    }

    @Override
    protected void del(String key) {
        /*ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            jedis.del(key);
        } finally {
            returnResource(shardedJedis);
        }*/
        try {
            jedisSharding.del(key);
        } finally {
            returnResource(jedisSharding);
        }
    }

}
