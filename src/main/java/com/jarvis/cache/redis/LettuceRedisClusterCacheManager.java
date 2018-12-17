package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public class LettuceRedisClusterCacheManager extends AbstractRedisCacheManager {

    private final RedisClusterClient redisClusterClient;

    public LettuceRedisClusterCacheManager(RedisClusterClient redisClusterClient, ISerializer<Object> serializer) {
        super(serializer);
        this.redisClusterClient = redisClusterClient;
    }

    @Override
    protected IRedis getRedis() {
        StatefulRedisClusterConnection<byte[], byte[]> connection = redisClusterClient.connect(ByteArrayCodec.INSTANCE);
        return new LettuceRedisClusterClient(connection, this);
    }

    public static class LettuceRedisClusterClient implements IRedis {

        private final StatefulRedisClusterConnection<byte[], byte[]> connection;

        private final AbstractRedisCacheManager cacheManager;

        public LettuceRedisClusterClient(StatefulRedisClusterConnection<byte[], byte[]> connection, AbstractRedisCacheManager cacheManager) {
            this.connection = connection;
            this.cacheManager = cacheManager;
        }

        @Override
        public void close() throws IOException {
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
            // 为了提升性能，开启pipeline
            this.connection.setAutoFlushCommands(false);
            RedisAdvancedClusterAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
            asyncCommands.hset(key, field, value);
            asyncCommands.expire(key, seconds);
            this.connection.flushCommands();
        }

        @Override
        public void mset(Collection<MSetParam> params) {
            // 为了提升性能，开启pipeline
            this.connection.setAutoFlushCommands(false);
            RedisAdvancedClusterAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
            try {
                LettuceRedisUtil.executeMSet((AbstractRedisAsyncCommands<byte[], byte[]>) asyncCommands, cacheManager, params);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                this.connection.flushCommands();
            }
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
        public Map<CacheKeyTO, CacheWrapper<Object>> mget(Type returnType, Set<CacheKeyTO> keys) {
            RedisAdvancedClusterAsyncCommands<byte[], byte[]> asyncCommands = connection.async();
            return LettuceRedisUtil.executeMGet(connection, (AbstractRedisAsyncCommands<byte[], byte[]>) asyncCommands, cacheManager, returnType, keys);
        }

        @Override
        public void delete(Set<CacheKeyTO> keys) {
            // 为了提升性能，开启pipeline
            this.connection.setAutoFlushCommands(false);
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
            } finally {
                this.connection.flushCommands();
            }
        }

    }

}
