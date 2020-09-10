package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Redis缓存管理
 *
 *
 */
@Slf4j
public class ShardedJedisCacheManager extends AbstractRedisCacheManager {

    private final ShardedJedisPool shardedJedisPool;

    public ShardedJedisCacheManager(ShardedJedisPool shardedJedisPool, ISerializer<Object> serializer) {
        super(serializer);
        this.shardedJedisPool = shardedJedisPool;
    }

    @Override
    protected IRedis getRedis() {
        return new ShardedJedisClient(shardedJedisPool.getResource(), this);
    }

    public static class ShardedJedisClient implements IRedis {

        private final ShardedJedis shardedJedis;

        private final AbstractRedisCacheManager cacheManager;

        public ShardedJedisClient(ShardedJedis shardedJedis, AbstractRedisCacheManager cacheManager) {
            this.shardedJedis = shardedJedis;
            this.cacheManager = cacheManager;
        }

        @Override
        public void close() throws IOException {
            if (null != shardedJedis) {
                shardedJedis.close();
            }
        }

        @Override
        public void set(byte[] key, byte[] value) {
            Jedis jedis = shardedJedis.getShard(key);
            jedis.set(key, value);
        }

        @Override
        public void setex(byte[] key, int seconds, byte[] value) {
            Jedis jedis = shardedJedis.getShard(key);
            jedis.setex(key, seconds, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value) {
            Jedis jedis = shardedJedis.getShard(key);
            jedis.hset(key, field, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value, int seconds) {
            Jedis jedis = shardedJedis.getShard(key);
            Pipeline pipeline = jedis.pipelined();
            pipeline.hset(key, field, value);
            pipeline.expire(key, seconds);
            pipeline.sync();
        }

        @Override
        public void mset(Collection<MSetParam> params) {
            ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
            pipeline.setShardedJedis(shardedJedis);
            try {
                JedisUtil.executeMSet(pipeline, this.cacheManager, params);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            pipeline.sync();
        }

        @Override
        public byte[] get(byte[] key) {
            Jedis jedis = shardedJedis.getShard(key);
            return jedis.get(key);
        }

        @Override
        public byte[] hget(byte[] key, byte[] field) {
            Jedis jedis = shardedJedis.getShard(key);
            return jedis.hget(key, field);
        }

        @Override
        public Map<CacheKeyTO, CacheWrapper<Object>> mget(Type returnType, Set<CacheKeyTO> keys) throws Exception {
            ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
            pipeline.setShardedJedis(shardedJedis);
            JedisUtil.executeMGet(pipeline, keys);
            Collection<Object> values = pipeline.syncAndReturnAll();
            return cacheManager.deserialize(keys, values, returnType);
        }

        @Override
        public void delete(Set<CacheKeyTO> keys) {
            ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
            pipeline.setShardedJedis(shardedJedis);
            JedisUtil.executeDelete(pipeline, keys);
            pipeline.sync();
        }

    }
}
