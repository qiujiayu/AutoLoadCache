package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulConnection;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Slf4j
public class LettuceRedisUtil {

    public static void executeMSet(AbstractRedisAsyncCommands<byte[], byte[]> pipeline, AbstractRedisCacheManager manager, Collection<MSetParam> params) throws Exception {
        CacheKeyTO cacheKeyTO;
        String cacheKey;
        String hfield;
        CacheWrapper<Object> result;
        byte[] key;
        byte[] val;
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
            val = manager.getSerializer().serialize(result);
            if (null == hfield || hfield.isEmpty()) {
                int expire = result.getExpire();
                if (expire == AbstractRedisCacheManager.NEVER_EXPIRE) {
                    pipeline.set(key, val);
                } else if (expire > 0) {
                    pipeline.setex(key, expire, val);
                }
            } else {
                int hExpire = manager.getHashExpire() < 0 ? result.getExpire() : manager.getHashExpire();
                pipeline.hset(key, AbstractRedisCacheManager.KEY_SERIALIZER.serialize(hfield), val);
                if (hExpire > 0) {
                    pipeline.expire(key, hExpire);
                }
            }
        }
    }

    public static Map<CacheKeyTO, CacheWrapper<Object>> executeMGet(StatefulConnection<byte[], byte[]> connection, AbstractRedisAsyncCommands<byte[], byte[]> asyncCommands, AbstractRedisCacheManager cacheManager, Type returnType, Set<CacheKeyTO> keys) {
        String hfield;
        String cacheKey;
        byte[] key;
        RedisFuture<byte[]>[] futures = new RedisFuture[keys.size()];
        try {
            // 为了提升性能，开启pipeline
            connection.setAutoFlushCommands(false);
            int i = 0;
            for (CacheKeyTO cacheKeyTO : keys) {
                cacheKey = cacheKeyTO.getCacheKey();
                if (null == cacheKey || cacheKey.isEmpty()) {
                    continue;
                }
                hfield = cacheKeyTO.getHfield();
                key = AbstractRedisCacheManager.KEY_SERIALIZER.serialize(cacheKey);
                if (null == hfield || hfield.isEmpty()) {
                    futures[i] = asyncCommands.get(key);
                } else {
                    futures[i] = asyncCommands.hget(key, AbstractRedisCacheManager.KEY_SERIALIZER.serialize(hfield));
                }
                i++;
            }
        } finally {
            connection.flushCommands();
        }
        Map<CacheKeyTO, CacheWrapper<Object>> res = new HashMap<>(keys.size());
        int i = 0;
        for (CacheKeyTO cacheKeyTO : keys) {
            RedisFuture<byte[]> future = futures[i];
            try {
                byte[] data = future.get();
                if (null == data || data.length == 0) {
                    continue;
                }
                CacheWrapper<Object> value = (CacheWrapper<Object>)cacheManager.getSerializer().deserialize(data, returnType);
                if (null != value) {
                    res.put(cacheKeyTO, value);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            i++;
        }
        return res;
    }
}
