package com.jarvis.cache.redis;

import com.jarvis.cache.lock.AbstractRedisLock;
import com.jarvis.cache.serializer.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.types.Expiration;

import java.util.concurrent.TimeUnit;

public class SpringRedisLock extends AbstractRedisLock {
    private static final Logger logger = LoggerFactory.getLogger(SpringRedisLock.class);
    private static final StringSerializer STRING_SERIALIZER = new StringSerializer();

    private final RedisConnectionFactory redisConnectionFactory;

    public SpringRedisLock(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    private RedisConnection getConnection() {
        return RedisConnectionUtils.getConnection(redisConnectionFactory);
    }

    @Override
    protected boolean setnx(String key, String val, int expire) {
        if (null == redisConnectionFactory || null == key || key.isEmpty()) {
            return false;
        }
        RedisConnection redisConnection = getConnection();
        try {
            Expiration expiration = Expiration.from(expire, TimeUnit.SECONDS);
            // 采用redisson做客户端时，set key value [EX | PX] [NX | XX] 会因为条件不满足无法设值成功而返回null导致拆箱空指针
            Boolean locked = redisConnection.stringCommands().set(STRING_SERIALIZER.serialize(key), STRING_SERIALIZER.serialize(val), expiration, RedisStringCommands.SetOption.SET_IF_ABSENT);
            return locked == null ? false : locked;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            RedisConnectionUtils.releaseConnection(redisConnection, redisConnectionFactory);
        }
        return false;
    }

    @Override
    protected void del(String key) {
        if (null == redisConnectionFactory || null == key || key.length() == 0) {
            return;
        }
        RedisConnection redisConnection = getConnection();
        try {
            redisConnection.keyCommands().del(STRING_SERIALIZER.serialize(key));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            RedisConnectionUtils.releaseConnection(redisConnection, redisConnectionFactory);
        }
    }

}
