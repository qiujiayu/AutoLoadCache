package com.jarvis.cache.redis;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.MSetParam;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.serializer.StringSerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 *
 */
@Getter
@Slf4j
public abstract class AbstractRedisCacheManager implements ICacheManager {

    public static final StringSerializer KEY_SERIALIZER = new StringSerializer();

    /**
     * Hash的缓存时长：等于0时永久缓存；大于0时，主要是为了防止一些已经不用的缓存占用内存;hashExpire小于0时，则使用@Cache中设置的expire值（默认值为-1）。
     */
    protected int hashExpire = -1;

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
        if (null == cacheKey || cacheKey.isEmpty()) {
            return;
        }
        try (IRedis redis = getRedis()) {
            String hfield = cacheKeyTO.getHfield();
            byte[] key = KEY_SERIALIZER.serialize(cacheKey);
            byte[] val = serializer.serialize(result);
            if (null == hfield || hfield.isEmpty()) {
                int expire = result.getExpire();
                if (expire == NEVER_EXPIRE) {
                    redis.set(key, val);
                } else if (expire > 0) {
                    redis.setex(key, expire, val);
                }
            } else {
                byte[] field = KEY_SERIALIZER.serialize(hfield);
                int hExpire = hashExpire < 0 ? result.getExpire() : hashExpire;
                if (hExpire == NEVER_EXPIRE) {
                    redis.hset(key, field, val);
                } else if (hExpire > 0) {
                    redis.hset(key, field, val, hExpire);
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    @Override
    public void mset(final Method method, final Collection<MSetParam> params) {
        if (null == params || params.isEmpty()) {
            return;
        }
        try (IRedis redis = getRedis()) {
            redis.mset(params);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(final CacheKeyTO cacheKeyTO, final Method method) throws CacheCenterConnectionException {
        if (null == cacheKeyTO) {
            return null;
        }
        String cacheKey = cacheKeyTO.getCacheKey();
        if (null == cacheKey || cacheKey.isEmpty()) {
            return null;
        }
        CacheWrapper<Object> res = null;
        String hfield;
        Type returnType = null;
        try (IRedis redis = getRedis()) {
            byte[] val;
            hfield = cacheKeyTO.getHfield();
            if (null == hfield || hfield.isEmpty()) {
                val = redis.get(KEY_SERIALIZER.serialize(cacheKey));
            } else {
                val = redis.hget(KEY_SERIALIZER.serialize(cacheKey), KEY_SERIALIZER.serialize(hfield));
            }
            if (null != method) {
                returnType = method.getGenericReturnType();
            }
            res = (CacheWrapper<Object>) serializer.deserialize(val, returnType);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return res;
    }

    @Override
    public Map<CacheKeyTO, CacheWrapper<Object>> mget(final Method method, final Type returnType, final Set<CacheKeyTO> keys) {
        if (null == keys || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        try (IRedis redis = getRedis()) {
            return redis.mget(returnType, keys);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    public Map<CacheKeyTO, CacheWrapper<Object>> deserialize(Set<CacheKeyTO> keys, Collection<Object> values, Type returnType) throws Exception {
        if (null == values || values.isEmpty()) {
            return Collections.emptyMap();
        }
        CacheWrapper<Object> tmp;
        Map<CacheKeyTO, CacheWrapper<Object>> res = new HashMap<>(keys.size());
        Iterator<CacheKeyTO> keysIt = keys.iterator();
        for (Object value : values) {
            CacheKeyTO cacheKeyTO = keysIt.next();
            if (null == value) {
                continue;
            }
            if (!(value instanceof byte[])) {
                log.warn("the data from redis is not byte[] but " + value.getClass().getName());
                continue;
            }
            tmp = (CacheWrapper<Object>) serializer.deserialize((byte[]) value, returnType);
            if (null != tmp) {
                res.put(cacheKeyTO, tmp);
            }
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
