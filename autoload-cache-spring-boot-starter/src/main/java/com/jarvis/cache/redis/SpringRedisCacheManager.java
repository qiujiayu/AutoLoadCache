package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;

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
public class SpringRedisCacheManager extends AbstractRedisCacheManager {

    private final RedisConnectionFactory redisConnectionFactory;

    public SpringRedisCacheManager(RedisConnectionFactory redisConnectionFactory, ISerializer<Object> serializer) {
        super(serializer);
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    protected IRedis getRedis() {
        return new RedisConnectionClient(redisConnectionFactory, this);
    }

    public static class RedisConnectionClient implements IRedis {
        private final RedisConnectionFactory redisConnectionFactory;
        private final RedisConnection redisConnection;

        private final AbstractRedisCacheManager cacheManager;

        public RedisConnectionClient(RedisConnectionFactory redisConnectionFactory, AbstractRedisCacheManager cacheManager) {
            this.redisConnectionFactory = redisConnectionFactory;
            this.redisConnection = RedisConnectionUtils.getConnection(redisConnectionFactory);
            // TransactionSynchronizationManager.hasResource(redisConnectionFactory);
            this.cacheManager = cacheManager;
        }

        @Override
        public void close() {
            RedisConnectionUtils.releaseConnection(redisConnection, redisConnectionFactory);
        }

        @Override
        public void set(byte[] key, byte[] value) {
            redisConnection.stringCommands().set(key, value);
        }

        @Override
        public void setex(byte[] key, int seconds, byte[] value) {
            redisConnection.stringCommands().setEx(key, seconds, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value) {
            redisConnection.hashCommands().hSet(key, field, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value, int seconds) {
            try {
                redisConnection.openPipeline();
                redisConnection.hashCommands().hSet(key, field, value);
                redisConnection.keyCommands().expire(key, seconds);
            } finally {
                redisConnection.closePipeline();
            }
        }

        @Override
        public void mset(Collection<MSetParam> params) throws Exception {
            CacheKeyTO cacheKeyTO;
            String cacheKey;
            String hfield;
            CacheWrapper<Object> result;
            byte[] key;
            byte[] val;
            try {
                redisConnection.openPipeline();
                for (MSetParam param : params) {
                    if (null == param) {
                        continue;
                    }
                    cacheKeyTO = param.getCacheKey();
                    cacheKey = cacheKeyTO.getCacheKey();
                    if (null == cacheKey || cacheKey.isEmpty()) {
                        continue;
                    }
                    result = param.getResult();
                    hfield = cacheKeyTO.getHfield();
                    key = AbstractRedisCacheManager.KEY_SERIALIZER.serialize(cacheKey);
                    val = cacheManager.getSerializer().serialize(result);
                    if (null == hfield || hfield.isEmpty()) {
                        int expire = result.getExpire();
                        if (expire == AbstractRedisCacheManager.NEVER_EXPIRE) {
                            redisConnection.stringCommands().set(key, val);
                        } else if (expire > 0) {
                            redisConnection.stringCommands().setEx(key, expire, val);
                        }
                    } else {
                        int hExpire = cacheManager.getHashExpire() < 0 ? result.getExpire() : cacheManager.getHashExpire();
                        redisConnection.hashCommands().hSet(key, AbstractRedisCacheManager.KEY_SERIALIZER.serialize(hfield), val);
                        if (hExpire > 0) {
                            redisConnection.keyCommands().expire(key, hExpire);
                        }
                    }
                }
            } finally {
                redisConnection.closePipeline();
            }
        }

        @Override
        public byte[] get(byte[] key) {
            return redisConnection.stringCommands().get(key);
        }

        @Override
        public byte[] hget(byte[] key, byte[] field) {
            return redisConnection.hashCommands().hGet(key, field);
        }

        @Override
        public Map<CacheKeyTO, CacheWrapper<Object>> mget(Type returnType, Set<CacheKeyTO> keys) throws Exception {
            String hfield;
            String cacheKey;
            byte[] key;
            try {
                redisConnection.openPipeline();
                for (CacheKeyTO cacheKeyTO : keys) {
                    cacheKey = cacheKeyTO.getCacheKey();
                    if (null == cacheKey || cacheKey.isEmpty()) {
                        continue;
                    }
                    hfield = cacheKeyTO.getHfield();
                    key = AbstractRedisCacheManager.KEY_SERIALIZER.serialize(cacheKey);
                    if (null == hfield || hfield.isEmpty()) {
                        redisConnection.stringCommands().get(key);
                    } else {
                        redisConnection.hashCommands().hGet(key, AbstractRedisCacheManager.KEY_SERIALIZER.serialize(hfield));
                    }
                }
            } finally {
                return cacheManager.deserialize(keys, redisConnection.closePipeline(), returnType);
            }
        }

        @Override
        public void delete(Set<CacheKeyTO> keys) {
            try {
                redisConnection.openPipeline();
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
                        redisConnection.keyCommands().del(KEY_SERIALIZER.serialize(cacheKey));
                    } else {
                        redisConnection.hashCommands().hDel(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
                    }
                }
            } finally {
                redisConnection.closePipeline();
            }

        }


    }

}
