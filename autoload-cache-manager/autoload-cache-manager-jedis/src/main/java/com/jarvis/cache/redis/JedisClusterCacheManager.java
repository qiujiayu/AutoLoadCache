package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;

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
public class JedisClusterCacheManager extends AbstractRedisCacheManager {

    private final JedisClusterClient redis;

    public JedisClusterCacheManager(JedisCluster jedisCluster, ISerializer<Object> serializer) {
        super(serializer);
        this.redis = new JedisClusterClient(jedisCluster, this);
    }

    @Override
    protected IRedis getRedis() {
        return redis;
    }

    public static class JedisClusterClient implements IRedis {

        private final JedisCluster jedisCluster;

        private final AbstractRedisCacheManager cacheManager;

        public JedisClusterClient(JedisCluster jedisCluster, AbstractRedisCacheManager cacheManager) {
            this.jedisCluster = jedisCluster;
            this.cacheManager = cacheManager;
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
            try {
                retryableJedisClusterPipeline.sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        @Override
        public void mset(Collection<MSetParam> params) {
            RetryableJedisClusterPipeline retryableJedisClusterPipeline = new RetryableJedisClusterPipeline(jedisCluster) {
                @Override
                public void execute(JedisClusterPipeline pipeline) throws Exception {
                    JedisUtil.executeMSet(pipeline, cacheManager, params);
                }
            };
            try {
                retryableJedisClusterPipeline.sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
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
        public Map<CacheKeyTO, CacheWrapper<Object>> mget(Type returnType, Set<CacheKeyTO> keys) throws Exception {
            RetryableJedisClusterPipeline retryableJedisClusterPipeline = new RetryableJedisClusterPipeline(jedisCluster) {
                @Override
                public void execute(JedisClusterPipeline pipeline) {
                    JedisUtil.executeMGet(pipeline, keys);
                }
            };
            return cacheManager.deserialize(keys, retryableJedisClusterPipeline.syncAndReturnAll(), returnType);
        }

        @Override
        public void delete(Set<CacheKeyTO> keys) {
            RetryableJedisClusterPipeline retryableJedisClusterPipeline = new RetryableJedisClusterPipeline(jedisCluster) {
                @Override
                public void execute(JedisClusterPipeline pipeline) {
                    JedisUtil.executeDelete(pipeline, keys);
                }
            };
            try {
                retryableJedisClusterPipeline.sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

}
