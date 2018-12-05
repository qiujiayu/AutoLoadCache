package com.jarvis.cache.redis;

import com.jarvis.cache.MSetParam;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.PipelineBase;

import java.util.Collection;
import java.util.Set;

@Slf4j
public class JedisUtil {

    public static void executeMSet(PipelineBase pipeline, AbstractRedisCacheManager manager, Collection<MSetParam> params) throws Exception {
        CacheKeyTO cacheKeyTO;
        String cacheKey;
        String hfield;
        CacheWrapper<Object> result;
        byte[] key;
        byte[] val;
        for (MSetParam param : params) {
            if(null == param){
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

    public static void executeMGet(PipelineBase pipeline, Set<CacheKeyTO> keys) {
        String hfield;
        String cacheKey;
        byte[] key;
        for (CacheKeyTO cacheKeyTO : keys) {
            cacheKey = cacheKeyTO.getCacheKey();
            if (null == cacheKey || cacheKey.isEmpty()) {
                continue;
            }
            hfield = cacheKeyTO.getHfield();
            key = AbstractRedisCacheManager.KEY_SERIALIZER.serialize(cacheKey);
            if (null == hfield || hfield.isEmpty()) {
                pipeline.get(key);
            } else {
                pipeline.hget(key, AbstractRedisCacheManager.KEY_SERIALIZER.serialize(hfield));
            }
        }
    }

    public static void executeDelete(PipelineBase pipeline, Set<CacheKeyTO> keys) {
        String hfield;
        String cacheKey;
        byte[] key;
        for (CacheKeyTO cacheKeyTO : keys) {
            cacheKey = cacheKeyTO.getCacheKey();
            if (null == cacheKey || cacheKey.isEmpty()) {
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug("delete cache {}", cacheKey);
            }
            hfield = cacheKeyTO.getHfield();
            key = AbstractRedisCacheManager.KEY_SERIALIZER.serialize(cacheKey);
            if (null == hfield || hfield.isEmpty()) {
                pipeline.del(key);
            } else {
                pipeline.hdel(key, AbstractRedisCacheManager.KEY_SERIALIZER.serialize(hfield));
            }
        }

    }
}
