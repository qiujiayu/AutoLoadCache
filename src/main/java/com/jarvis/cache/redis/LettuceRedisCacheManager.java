package com.jarvis.cache.redis;

import java.io.IOException;

import com.jarvis.cache.serializer.ISerializer;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LettuceRedisCacheManager extends AbstractRedisCacheManager {

    private final RedisClient redisClient;

    private final ByteArrayCodec byteArrayCodec = new ByteArrayCodec();

    public LettuceRedisCacheManager(RedisClient redisClient, ISerializer<Object> serializer) {
        super(serializer);
        this.redisClient = redisClient;
    }

    @Override
    protected IRedis getRedis(String cacheKey) {
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(byteArrayCodec);
        return new LettuceRedisClient(connection);
    }

    public static class LettuceRedisClient implements IRedis {

        private final StatefulRedisConnection<byte[], byte[]> connection;

        public LettuceRedisClient(StatefulRedisConnection<byte[], byte[]> connection) {
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
