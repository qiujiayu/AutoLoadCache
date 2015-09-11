package com.jarvis.cache.redis;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.AutoLoadHandler;
import com.jarvis.cache.CacheUtil;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存切面，用于拦截数据并调用Redis进行缓存
 * @author jiayu.qiu
 * @see com.jarvis.cache.redis.ShardedCachePointCut
 * @deprecated 建议使用com.jarvis.cache.redis.ShardedCachePointCut
 */
@Deprecated
public class CachePointCut extends AbstractCacheManager<Serializable> {

    private static final Logger logger=Logger.getLogger(CachePointCut.class);

    private List<RedisTemplate<String, Serializable>> redisTemplateList;

    private StringRedisSerializer keySerializer=new StringRedisSerializer();

    private JdkSerializationRedisSerializer valSerializer=new JdkSerializationRedisSerializer();

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
        if(cacheKey.indexOf("*") != -1 || cacheKey.indexOf("?") != -1) {
            throw new RuntimeException("cacheKey:" + cacheKey + "; has '*' or '?'");
        }
        try {
            result.setLastLoadTime(System.currentTimeMillis());
            final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
            redisTemplate.execute(new RedisCallback<Object>() {

                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {

                    try {
                        byte[] key=keySerializer.serialize(cacheKey);
                        byte[] val=valSerializer.serialize(result);
                        connection.setEx(key, expire, val);
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }

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

                    byte[] key=keySerializer.serialize(cacheKey);

                    byte[] value=connection.get(key);
                    if(null != value && value.length > 0) {

                        try {
                            @SuppressWarnings("unchecked")
                            CacheWrapper<Serializable> res=(CacheWrapper<Serializable>)valSerializer.deserialize(value);
                            return res;
                        } catch(Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }

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
     * @param method 方法名
     * @param arguments 参数
     * @param subKeySpEL SpringEL表达式，arguments 在SpringEL表达式中的名称为args，第一个参数为#args[0],第二个为参数为#args[1]，依此类推。
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
        if(null == redisTemplateList || redisTemplateList.isEmpty() || null == cacheKey) {
            return;
        }
        final AutoLoadHandler<Serializable> autoLoadHandler=this.getAutoLoadHandler();
        String params[]=new String[]{cacheKey};
        final byte[][] p=new byte[params.length][];
        for(int i=0; i < params.length; i++) {
            p[i]=keySerializer.serialize(params[i]);
        }
        if(cacheKey.indexOf("*") != -1 || cacheKey.indexOf("?") != -1) {
            final StringBuilder script=new StringBuilder();
            script.append("local keys = redis.call('keys', KEYS[1]);\n");
            script.append("if(not keys or #keys == 0) then \n return nil; \n end \n");
            script.append("redis.call('del', unpack(keys)); \n return keys;");

            for(final RedisTemplate<String, Serializable> redisTemplate: redisTemplateList) {
                redisTemplate.execute(new RedisCallback<Object>() {

                    @Override
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        byte[] scriptBytes=keySerializer.serialize(script.toString());
                        connection.eval(scriptBytes, ReturnType.STATUS, 1, p);
                        return null;
                    }
                });
            }
        } else {
            final RedisTemplate<String, Serializable> redisTemplate=getRedisTemplate(cacheKey);
            redisTemplate.execute(new RedisCallback<Object>() {

                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] key=keySerializer.serialize(cacheKey);
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
