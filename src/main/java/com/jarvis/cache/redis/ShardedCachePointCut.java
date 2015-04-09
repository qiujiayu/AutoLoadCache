package com.jarvis.cache.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.AutoLoadHandler;
import com.jarvis.cache.CacheUtil;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用Redis进行缓存
 * @author jiayu.qiu
 */
public class ShardedCachePointCut extends AbstractCacheManager<Serializable> {

    private static final Logger logger=Logger.getLogger(ShardedCachePointCut.class);

    private static final StringRedisSerializer keySerializer=new StringRedisSerializer();

    private static final JdkSerializationRedisSerializer valueSerializer=new JdkSerializationRedisSerializer();

    private ShardedJedisPool shardedJedisPool;

    public ShardedCachePointCut(AutoLoadConfig config) {
        super(config);
    }

    private void returnResource(ShardedJedis shardedJedis) {
        shardedJedis.close();
    }

    @Override
    public void setCache(final String cacheKey, final CacheWrapper<Serializable> result, final int expire) {
        if(null == shardedJedisPool || null == cacheKey) {
            return;
        }
        if(cacheKey.indexOf("*") != -1 || cacheKey.indexOf("?") != -1) {
            throw new java.lang.RuntimeException("cacheKey:" + cacheKey + "; has '*' or '?'");
        }
        ShardedJedis shardedJedis=null;
        try {
            result.setLastLoadTime(System.currentTimeMillis());
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            jedis.setex(keySerializer.serialize(cacheKey), expire, valueSerializer.serialize(result));
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            returnResource(shardedJedis);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Serializable> get(final String cacheKey) {
        if(null == shardedJedisPool || null == cacheKey) {
            return null;
        }
        CacheWrapper<Serializable> res=null;
        ShardedJedis shardedJedis=null;
        try {
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            byte bytes[]=jedis.get(keySerializer.serialize(cacheKey));
            res=(CacheWrapper<Serializable>)valueSerializer.deserialize(bytes);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            returnResource(shardedJedis);
        }
        return res;
    }

    /**
     * 根据默认缓存Key删除缓存
     * @param cs Class
     * @param method
     * @param arguments
     * @param subKeySpEL
     * @param deleteByPrefixKey 是否批量删除
     */
    public void deleteByDefaultCacheKey(@SuppressWarnings("rawtypes") Class cs, String method, Object[] arguments,
        String subKeySpEL, boolean deleteByPrefixKey) {
        try {
            String cacheKey;
            if(deleteByPrefixKey) {
                cacheKey=CacheUtil.getDefaultCacheKeyPrefix(cs.getName(), method, arguments, subKeySpEL) + "*";
            } else {
                cacheKey=CacheUtil.getDefaultCacheKey(cs.getName(), method, arguments, subKeySpEL);
            }
            delete(cacheKey);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * 通过Spring EL 表达式，删除缓存
     * @param keySpEL Spring EL表达式
     * @param arguments 参数
     */
    public void deleteDefinedCacheKey(String keySpEL, Object[] arguments) {
        String cacheKey=CacheUtil.getDefinedCacheKey(keySpEL, arguments);
        this.delete(cacheKey);
    }

    /**
     * 根据缓存Key删除缓存
     * @param cacheKey 如果传进来的值中 带有 * 或 ? 号，则会使用批量删除（遍历所有Redis服务器）
     */
    @Override
    public void delete(final String cacheKey) {
        if(null == shardedJedisPool || null == cacheKey) {
            return;
        }
        final AutoLoadHandler<Serializable> autoLoadHandler=this.getAutoLoadHandler();
        ShardedJedis shardedJedis=null;
        if(cacheKey.indexOf("*") != -1 || cacheKey.indexOf("?") != -1) {// 如果是批量删除缓存，则要遍历所有redis，避免遗漏。
            try {
                shardedJedis=shardedJedisPool.getResource();
                Collection<Jedis> list=shardedJedis.getAllShards();
                for(Jedis jedis: list) {
                    Set<byte[]> keys=jedis.keys(keySerializer.serialize(cacheKey));
                    if(null != keys && keys.size() > 0) {
                        byte[][] keys2=new byte[keys.size()][];
                        keys.toArray(keys2);
                        jedis.del(keys2);
                        for(byte[] tmp: keys2) {
                            String tmpKey=(String)keySerializer.deserialize(tmp);
                            autoLoadHandler.resetAutoLoadLastLoadTime(tmpKey);
                        }
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
                jedis.del(keySerializer.serialize(cacheKey));
                autoLoadHandler.resetAutoLoadLastLoadTime(cacheKey);
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
