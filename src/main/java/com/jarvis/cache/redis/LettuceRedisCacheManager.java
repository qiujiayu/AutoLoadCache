package com.jarvis.cache.redis;

import java.io.IOException;
import java.util.Set;

import com.jarvis.cache.serializer.ISerializer;

import com.jarvis.cache.to.CacheKeyTO;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LettuceRedisCacheManager extends AbstractRedisCacheManager {

    private final RedisClient redisClient;

    public LettuceRedisCacheManager(RedisClient redisClient, ISerializer<Object> serializer) {
        super(serializer);
        this.redisClient = redisClient;
    }

    @Override
    protected IRedis getRedis() {
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        return new LettuceRedisClient(connection);
    }

    public static class LettuceRedisClient implements IRedis {

        private final StatefulRedisConnection<byte[], byte[]> connection;

        public LettuceRedisClient(StatefulRedisConnection<byte[], byte[]> connection) {
            this.connection = connection;
            // 为了提升性能，开启pipeline
            this.connection.setAutoFlushCommands(false);
        }

        @Override
        public void close() throws IOException {
            this.connection.flushCommands();
            this.connection.close();
        }

        @Override
        public void set(byte[] key, byte[] value) {
            connection.async().set(key, value);
        }

        @Override
        public void setex(byte[] key, int seconds, byte[] value) {
            connection.async().setex(key, seconds, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value) {
            connection.async().hset(key, field, value);
        }

        @Override
        public void hset(byte[] key, byte[] field, byte[] value, int seconds) {
            RedisAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
            asyncCommands.hset(key, field, value);
            asyncCommands.expire(key, seconds);
        }

        @Override
        public byte[] get(byte[] key) {
            try {
                return connection.async().get(key).get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }

        @Override
        public byte[] hget(byte[] key, byte[] field) {
            try {
                return connection.async().hget(key, field).get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }

        @Override
        public void delete(Set<CacheKeyTO> keys) {
            if (null == keys || keys.isEmpty()) {
                return;
            }
            RedisAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
            try {
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
                        asyncCommands.del(KEY_SERIALIZER.serialize(cacheKey));
                    } else {
                        asyncCommands.hdel(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
