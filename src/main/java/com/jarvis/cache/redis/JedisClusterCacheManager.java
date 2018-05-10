package com.jarvis.cache.redis;

import java.io.IOException;

import com.jarvis.cache.serializer.ISerializer;

import redis.clients.jedis.JedisCluster;

/**
 * Redis缓存管理
 * 
 * @author jiayu.qiu
 */
public class JedisClusterCacheManager extends AbstractRedisCacheManager {

    private final JedisClusterClient redis;

    public JedisClusterCacheManager(JedisCluster jedisCluster, ISerializer<Object> serializer) {
        super(serializer);
        this.redis = new JedisClusterClient(jedisCluster);
    }

    @Override
    protected IRedis getRedis(String cacheKey) {
        return redis;
    }

    public static class JedisClusterClient implements IRedis {

        private final JedisCluster jedisCluster;

        public JedisClusterClient(JedisCluster jedisCluster) {
            this.jedisCluster = jedisCluster;
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public void set(byte[] key, byte[] value) {
            jedisCluster.set(key, value);
        }

        @Override
        public void setex(byte[] key, int seconds, byte[] value) {
            jedisCluster.setex(key, seconds, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value) {
            jedisCluster.hset(key, field, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value, int seconds) {
            jedisCluster.hset(key, field, value);
            jedisCluster.expire(key, seconds);
        }

        @Override
        public byte[] get(byte[] key) {
            return jedisCluster.get(key);
        }

        @Override
        public byte[] hget(byte[] key, byte[] field) {
            return jedisCluster.hget(key, field);
        }

        @Override
        public void del(byte[] key) {
            jedisCluster.del(key);
        }

        @Override
        public void hdel(byte[] key, byte[]... fields) {
            jedisCluster.hdel(key, fields);
        }

    }

}
