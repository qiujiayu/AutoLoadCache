package com.jarvis.cache.redis;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.AutoLoadHandler;
import com.jarvis.cache.CacheUtil;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用Redis进行缓存
 * @author jiayu.qiu
 */
public class CachePointCut extends AbstractCacheManager<Serializable> {

    private static final Logger logger=Logger.getLogger(CachePointCut.class);

    private List<RedisTemplate<String, Serializable>> redisTemplateList;

    public CachePointCut(AutoLoadConfig config) {
        super(config);
    }

    public RedisTemplate<String, Serializable> getRedisTemplate(String key) {
        if(null == redisTemplateList || redisTemplateList.isEmpty()) {
            return null;
        }
        int hash=Math.abs(key.hashCode());
        Integer clientKey=hash % redisTemplateList.size();
        return redisTemplateList.get(clientKey);
    }

    @Override
    public void setCache(final String cacheKey, final CacheWrapper<Serializable> result, final int expire) {
        try {
            final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
            redisTemplate.execute(new RedisCallback<Object>() {

                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);
                    JdkSerializationRedisSerializer serializer=(JdkSerializationRedisSerializer)redisTemplate.getValueSerializer();
                    byte[] val=serializer.serialize(result);
                    connection.set(key, val);
                    connection.expire(key, expire);
                    return null;
                }
            });
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public CacheWrapper<Serializable> get(final String cacheKey) {
        CacheWrapper<Serializable> res=null;
        try {
            final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
            res=redisTemplate.execute(new RedisCallback<CacheWrapper<Serializable>>() {

                @Override
                public CacheWrapper<Serializable> doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);

                    byte[] value=connection.get(key);
                    if(null != value && value.length > 0) {
                        JdkSerializationRedisSerializer serializer=
                            (JdkSerializationRedisSerializer)redisTemplate.getValueSerializer();
                        @SuppressWarnings("unchecked")
                        CacheWrapper<Serializable> res=(CacheWrapper<Serializable>)serializer.deserialize(value);
                        return res;
                    }
                    return null;
                }
            });
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
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
     * @param cacheKey 如果传进来的值中 带有 * 号，则会使用批量删除（遍历所有Redis服务器）
     */
    @Override
    public void delete(final String cacheKey) {
        if(null == redisTemplateList || redisTemplateList.isEmpty()) {
            return;
        }
        final AutoLoadHandler<Serializable> autoLoadHandler=this.getAutoLoadHandler();
        if(cacheKey.indexOf("*") != -1) {
            for(final RedisTemplate<String, Serializable> redisTemplate: redisTemplateList) {
                redisTemplate.execute(new RedisCallback<Object>() {

                    @Override
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);
                        Set<byte[]> keys=connection.keys(key);
                        if(null != keys && keys.size() > 0) {
                            byte[][] keys2=new byte[keys.size()][];
                            keys.toArray(keys2);
                            connection.del(keys2);

                            for(byte[] tmp: keys2) {
                                JdkSerializationRedisSerializer serializer=
                                    (JdkSerializationRedisSerializer)redisTemplate.getValueSerializer();
                                String tmpKey=(String)serializer.deserialize(tmp);
                                autoLoadHandler.resetAutoLoadLastLoadTime(tmpKey);
                            }
                        }
                        return null;
                    }
                });
            }
        } else {
            final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
            redisTemplate.execute(new RedisCallback<Object>() {

                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] key=redisTemplate.getStringSerializer().serialize(cacheKey);

                    connection.del(key);
                    autoLoadHandler.resetAutoLoadLastLoadTime(cacheKey);
                    return null;
                }
            });
        }
    }

    public List<RedisTemplate<String, Serializable>> getRedisTemplateList() {
        return redisTemplateList;
    }

    public void setRedisTemplateList(List<RedisTemplate<String, Serializable>> redisTemplateList) {
        this.redisTemplateList=redisTemplateList;
    }

}
