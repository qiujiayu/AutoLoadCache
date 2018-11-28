package com.jarvis.cache.redis;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.serializer.StringSerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: jiayu.qiu
 */
@Slf4j
public abstract class AbstractRedisCacheManager implements ICacheManager {

    protected static final StringSerializer KEY_SERIALIZER = new StringSerializer();

    /**
     * Hash的缓存时长：等于0时永久缓存；大于0时，主要是为了防止一些已经不用的缓存占用内存;hashExpire小于0时，则使用@Cache中设置的expire值（默认值为-1）。
     */
    private int hashExpire = -1;

    protected final ISerializer<Object> serializer;

    public AbstractRedisCacheManager(ISerializer<Object> serializer) {
        this.serializer = serializer;
    }

    protected abstract IRedis getRedis();

    @Override
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        try (IRedis redis = getRedis()) {
            int expire = result.getExpire();
            String hfield = cacheKeyTO.getHfield();
            if (null == hfield || hfield.length() == 0) {
                if (expire == 0) {
                    redis.set(KEY_SERIALIZER.serialize(cacheKey), serializer.serialize(result));
                } else if (expire > 0) {
                    redis.setex(KEY_SERIALIZER.serialize(cacheKey), expire, serializer.serialize(result));
                }
            } else {
                hashSet(redis, cacheKey, hfield, result);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void hashSet(IRedis redis, String cacheKey, String hfield, CacheWrapper<Object> result) throws Exception {
        byte[] key = KEY_SERIALIZER.serialize(cacheKey);
        byte[] field = KEY_SERIALIZER.serialize(hfield);
        byte[] val = serializer.serialize(result);
        int hExpire;
        if (hashExpire < 0) {
            hExpire = result.getExpire();
        } else {
            hExpire = hashExpire;
        }
        if (hExpire == 0) {
            redis.hset(key, field, val);
        } else if (hExpire > 0) {
            redis.hset(key, field, val, hExpire);
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
        try (IRedis redis = getRedis()) {
            byte[] bytes;
            hfield = cacheKeyTO.getHfield();
            if (null == hfield || hfield.length() == 0) {
                bytes = redis.get(KEY_SERIALIZER.serialize(cacheKey));
            } else {
                bytes = redis.hget(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
            }
            if (null != method) {
                returnType = method.getGenericReturnType();
            }
            res = (CacheWrapper<Object>) serializer.deserialize(bytes, returnType);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return res;
    }

    @Override
    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        if (null == keys || keys.isEmpty()) {
            return;
        }
        try (IRedis redis = getRedis()) {
            redis.delete(keys);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public int getHashExpire() {
        return hashExpire;
    }

    public void setHashExpire(int hashExpire) {
        if (hashExpire < 0) {
            return;
        }
        this.hashExpire = hashExpire;
    }
}
