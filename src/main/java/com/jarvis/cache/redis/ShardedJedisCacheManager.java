package com.jarvis.cache.redis;

import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

/**
 * Redis缓存管理
 *
 * @author jiayu.qiu
 */
@Slf4j
public class ShardedJedisCacheManager extends AbstractRedisCacheManager {

    private final ShardedJedisPool shardedJedisPool;

    public ShardedJedisCacheManager(ShardedJedisPool shardedJedisPool, ISerializer<Object> serializer) {
        super(serializer);
        this.shardedJedisPool = shardedJedisPool;
    }

    @Override
    protected IRedis getRedis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        try {
            Jedis jedis = shardedJedis.getShard(cacheKey);
            int expire = result.getExpire();
            String hfield = cacheKeyTO.getHfield();
            if (null == hfield || hfield.isEmpty()) {
                if (expire == 0) {
                    jedis.set(KEY_SERIALIZER.serialize(cacheKey), serializer.serialize(result));
                } else if (expire > 0) {
                    jedis.setex(KEY_SERIALIZER.serialize(cacheKey), expire, serializer.serialize(result));
                }
            } else {
                hashSet(jedis, cacheKey, hfield, result);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            shardedJedis.close();
        }
    }

    private void hashSet(Jedis jedis, String cacheKey, String hfield, CacheWrapper<Object> result) throws Exception {
        byte[] key = KEY_SERIALIZER.serialize(cacheKey);
        byte[] field = KEY_SERIALIZER.serialize(hfield);
        byte[] val = serializer.serialize(result);
        int hExpire;
        if (getHashExpire() < 0) {
            hExpire = result.getExpire();
        } else {
            hExpire = getHashExpire();
        }
        if (hExpire == 0) {
            jedis.hset(key, field, val);
        } else if (hExpire > 0) {
            Pipeline pipeline = jedis.pipelined();
            pipeline.hset(key, field, val);
            pipeline.expire(key, hExpire);
            pipeline.sync();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(final CacheKeyTO cacheKeyTO, final Method method) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return null;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.length() == 0) {
            return null;
        }
        CacheWrapper<Object> res = null;
        String hfield;
        Type returnType = null;
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        try {
            Jedis jedis = shardedJedis.getShard(cacheKey);
            byte[] bytes;
            hfield = cacheKeyTO.getHfield();
            if (null == hfield || hfield.length() == 0) {
                bytes = jedis.get(KEY_SERIALIZER.serialize(cacheKey));
            } else {
                bytes = jedis.hget(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
            }
            if (null != method) {
                returnType = method.getGenericReturnType();
            }
            res = (CacheWrapper<Object>) serializer.deserialize(bytes, returnType);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            shardedJedis.close();
        }
        return res;
    }

    @Override
    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        if (null == keys || keys.isEmpty()) {
            return;
        }
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        try {
            ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
            pipeline.setShardedJedis(shardedJedis);

            Iterator<CacheKeyTO> keysIterator = keys.iterator();
            CacheKeyTO cacheKeyTO;
            String cacheKey;
            String hfield;
            while (keysIterator.hasNext()) {
                cacheKeyTO = keysIterator.next();
                cacheKey = cacheKeyTO.getCacheKey();
                if (null == cacheKey || cacheKey.length() == 0) {
                    continue;
                }
                hfield = cacheKeyTO.getHfield();
                if (log.isDebugEnabled()) {
                    log.debug("delete cache {}, hfield {}", cacheKey, hfield);
                }
                if (null == hfield || hfield.isEmpty()) {
                    pipeline.del(KEY_SERIALIZER.serialize(cacheKey));
                } else {
                    pipeline.hdel(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
                }
            }
            pipeline.sync();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            shardedJedis.close();
        }
    }


}
