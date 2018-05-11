package com.jarvis.cache.redis;

import java.io.IOException;

import com.jarvis.cache.serializer.ISerializer;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
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
    protected IRedis getRedis(String cacheKey) {
        StatefulRedisClusterConnection<byte[], byte[]> connection = redisClusterClient.connect(byteArrayCodec);
        return new LettuceRedisClusterClient(connection);
    }

    public static class LettuceRedisClusterClient implements IRedis {

        private final StatefulRedisClusterConnection<byte[], byte[]> connection;

        public LettuceRedisClusterClient(StatefulRedisClusterConnection<byte[], byte[]> connection) {
            this.connection = connection;
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
            connection.async().hset(key, field, value).whenComplete((res, throwable) -> {
                if (res) {
                    connection.async().expire(key, seconds);
                }
            });

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
        public void del(byte[] key) {
            connection.async().del(key);
        }

        @Override
        public void hdel(byte[] key, byte[]... fields) {
            connection.async().hdel(key, fields);
        }

    }

}
