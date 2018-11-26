package com.jarvis.cache.redis;

import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.util.Set;

/**
 * Redis缓存管理
 *
 * @author jiayu.qiu
 */
@Slf4j
public class JedisClusterCacheManager extends AbstractRedisCacheManager {

    private final JedisClusterClient redis;

    public JedisClusterCacheManager(JedisCluster jedisCluster, ISerializer<Object> serializer) {
        super(serializer);
        this.redis = new JedisClusterClient(jedisCluster);
    }

    @Override
    protected IRedis getRedis() {
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
            RetryableJedisClusterPipeline retryableJedisClusterPipeline = new RetryableJedisClusterPipeline(jedisCluster) {
                @Override
                public void execute(JedisClusterPipeline pipeline) {
                    pipeline.hset(key, field, value);
                    pipeline.expire(key, seconds);
                }
            };
            retryableJedisClusterPipeline.sync();
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
        public void delete(Set<CacheKeyTO> keys) {
            if (null == keys || keys.isEmpty()) {
                return;
            }
            RetryableJedisClusterPipeline retryableJedisClusterPipeline = new RetryableJedisClusterPipeline(jedisCluster) {
                @Override
                public void execute(JedisClusterPipeline pipeline) {
                    for (CacheKeyTO cacheKeyTO : keys) {
                        String cacheKey = cacheKeyTO.getCacheKey();
                        if (null == cacheKey || cacheKey.length() == 0) {
                            continue;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("delete cache {}", cacheKey);
                        }
                        String hfield = cacheKeyTO.getHfield();
                        if (null == hfield || hfield.length() == 0) {
                            pipeline.del(KEY_SERIALIZER.serialize(cacheKey));
                        } else {
                            pipeline.hdel(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
                        }
                    }
                }
            };
            retryableJedisClusterPipeline.sync();
        }

    }

}
