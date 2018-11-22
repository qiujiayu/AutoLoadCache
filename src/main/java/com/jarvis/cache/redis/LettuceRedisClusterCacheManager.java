package com.jarvis.cache.redis;

import java.io.IOException;
import java.util.Set;

import com.jarvis.cache.serializer.ISerializer;

import com.jarvis.cache.to.CacheKeyTO;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LettuceRedisClusterCacheManager extends AbstractRedisCacheManager {

    private final RedisClusterClient redisClusterClient;

    private final ByteArrayCodec byteArrayCodec = new ByteArrayCodec();

    public LettuceRedisClusterCacheManager(RedisClusterClient redisClusterClient, ISerializer<Object> serializer) {
        super(serializer);
        this.redisClusterClient = redisClusterClient;
    }

    @Override
    protected IRedis getRedis() {
        StatefulRedisClusterConnection<byte[], byte[]> connection = redisClusterClient.connect(byteArrayCodec);
        return new LettuceRedisClusterClient(connection);
    }

    public static class LettuceRedisClusterClient implements IRedis {

        private final StatefulRedisClusterConnection<byte[], byte[]> connection;

        public LettuceRedisClusterClient(StatefulRedisClusterConnection<byte[], byte[]> connection) {
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
            RedisAdvancedClusterAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
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
            RedisAdvancedClusterAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
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
