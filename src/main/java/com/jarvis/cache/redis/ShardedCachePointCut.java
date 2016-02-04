package com.jarvis.cache.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.AutoLoadHandler;
import com.jarvis.cache.serializer.StringSerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用Redis进行缓存
 * @author jiayu.qiu
 */
public class ShardedCachePointCut extends AbstractCacheManager<Serializable> {

    private static final Logger logger=Logger.getLogger(ShardedCachePointCut.class);

    private static final StringSerializer keySerializer=new StringSerializer();

    private ShardedJedisPool shardedJedisPool;

    public ShardedCachePointCut(AutoLoadConfig config) {
        super(config);
    }

    private void returnResource(ShardedJedis shardedJedis) {
        shardedJedis.close();
    }

    @Override
    public void setCache(CacheKeyTO cacheKeyTO, final CacheWrapper<Serializable> result) {
        if(null == shardedJedisPool || null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        if(cacheKey.indexOf("*") != -1 || cacheKey.indexOf("?") != -1) {
            throw new java.lang.RuntimeException("cacheKey:" + cacheKey + "; has '*' or '?'");
        }
        ShardedJedis shardedJedis=null;
        try {
            int expire=result.getExpire();
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            String hfield=cacheKeyTO.getHfield();
            if(null == hfield || hfield.length() == 0) {
                if(expire == 0) {
                    jedis.set(keySerializer.serialize(cacheKey), getSerializer().serialize(result));
                } else {
                    jedis.setex(keySerializer.serialize(cacheKey), expire, getSerializer().serialize(result));
                }
            } else {
                jedis.hset(keySerializer.serialize(cacheKey), keySerializer.serialize(hfield), getSerializer().serialize(result));
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            returnResource(shardedJedis);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Serializable> get(CacheKeyTO cacheKeyTO) {
        if(null == shardedJedisPool || null == cacheKeyTO) {
            return null;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return null;
        }
        CacheWrapper<Serializable> res=null;
        ShardedJedis shardedJedis=null;
        try {
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            byte bytes[]=null;
            String hfield=cacheKeyTO.getHfield();
            if(null == hfield || hfield.length() == 0) {
                bytes=jedis.get(keySerializer.serialize(cacheKey));
            } else {
                bytes=jedis.hget(keySerializer.serialize(cacheKey), keySerializer.serialize(hfield));
            }
            res=(CacheWrapper<Serializable>)getSerializer().deserialize(bytes);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            returnResource(shardedJedis);
        }
        return res;
    }

    /**
     * 根据缓存Key删除缓存
     * @param cacheKeyTO 如果传进来的值中 带有 * 或 ? 号，则会使用批量删除（遍历所有Redis服务器）
     */
    @Override
    public void delete(CacheKeyTO cacheKeyTO) {
        if(null == shardedJedisPool || null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        logger.debug("delete cache:" + cacheKey);
        final AutoLoadHandler<Serializable> autoLoadHandler=this.getAutoLoadHandler();
        ShardedJedis shardedJedis=null;
        if(cacheKey.indexOf("*") != -1 || cacheKey.indexOf("?") != -1) {// 如果是批量删除缓存，则要遍历所有redis，避免遗漏。
            try {
                shardedJedis=shardedJedisPool.getResource();
                Collection<Jedis> list=shardedJedis.getAllShards();
                StringBuilder script=new StringBuilder();
                script.append("local keys = redis.call('keys', KEYS[1]);\n");
                script.append("if(not keys or #keys == 0) then \n return nil; \n end \n");
                script.append("redis.call('del', unpack(keys)); \n return keys;");
                for(Jedis jedis: list) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> keys=(List<String>)jedis.eval(script.toString(), 1, cacheKey);
                        if(null != keys && keys.size() > 0) {
                            /*
                             * for(String tmpKey: keys) { autoLoadHandler.resetAutoLoadLastLoadTime(tmpKey); }
                             */
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                returnResource(shardedJedis);
            }
        } else {
            try {
                shardedJedis=shardedJedisPool.getResource();
                Jedis jedis=shardedJedis.getShard(cacheKey);
                String hfield=cacheKeyTO.getHfield();
                if(null == hfield || hfield.length() == 0) {
                    jedis.del(keySerializer.serialize(cacheKey));
                } else {
                    jedis.hdel(keySerializer.serialize(cacheKey), keySerializer.serialize(hfield));
                }
                autoLoadHandler.resetAutoLoadLastLoadTime(cacheKeyTO);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                returnResource(shardedJedis);
            }
        }
    }

    public ShardedJedisPool getShardedJedisPool() {
        return shardedJedisPool;
    }

    public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool=shardedJedisPool;
    }

}
