package com.jarvis.cache.redis;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.serializer.StringSerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

/**
 * Redis缓存管理
 * @author jiayu.qiu
 */
public class JedisClusterCacheManager extends AbstractCacheManager implements InitializingBean {

    private static final Logger logger=Logger.getLogger(JedisClusterCacheManager.class);

    private static final StringSerializer keySerializer=new StringSerializer();

    private JedisCluster jedisCluster;

    private Integer timeout;

    private Integer maxRedirections;

    private String redisUrls;

    private GenericObjectPoolConfig genericObjectPoolConfig;

    /**
     * Hash的缓存时长：等于0时永久缓存；大于0时，主要是为了防止一些已经不用的缓存占用内存;hashExpire小于0时，则使用@Cache中设置的expire值（默认值为-1）。
     */
    private int hashExpire=-1;

    /**
     * 是否通过脚本来设置 Hash的缓存时长
     */
    private boolean hashExpireByScript=true;

    public JedisClusterCacheManager(AutoLoadConfig config, ISerializer<Object> serializer, AbstractScriptParser scriptParser) {
        super(config, serializer, scriptParser);
    }

    private Set<HostAndPort> parseHostAndPort() throws Exception {
        if(null == redisUrls || redisUrls.length() == 0) {
            return null;
        }
        try {
            String reids[]=redisUrls.split(";");
            Set<HostAndPort> haps=new HashSet<HostAndPort>();
            for(String redis: reids) {

                String[] ipAndPort=redis.split(":");

                try {
                    HostAndPort hap=new HostAndPort(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                    haps.add(hap);
                } catch(Exception ex) {
                    logger.error(ex);
                }
            }

            return haps;
        } catch(IllegalArgumentException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new Exception("解析 jedis 配置失败", ex);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Set<HostAndPort> haps=this.parseHostAndPort();
        if(null == haps || null == timeout || null == maxRedirections || null == genericObjectPoolConfig) {
            return;
        }
        logger.debug("new JedisCluster");
        jedisCluster=new JedisCluster(haps, timeout, maxRedirections, genericObjectPoolConfig);
    }

    @Override
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method, final Object args[]) throws CacheCenterConnectionException {
        if(null == jedisCluster || null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        try {
            int expire=result.getExpire();
            String hfield=cacheKeyTO.getHfield();
            if(null == hfield || hfield.length() == 0) {
                if(expire == 0) {
                    jedisCluster.set(keySerializer.serialize(cacheKey), getSerializer().serialize(result));
                } else {
                    jedisCluster.setex(keySerializer.serialize(cacheKey), expire, getSerializer().serialize(result));
                }
            } else {
                hashSet(cacheKey, hfield, result);
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
        }
    }

    private static byte[] hashSetScript;

    static {
        try {
            String tmpScript="redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]);\nredis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]));";
            hashSetScript=tmpScript.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void hashSet(String cacheKey, String hfield, CacheWrapper<Object> result) throws Exception {
        byte[] key=keySerializer.serialize(cacheKey);
        byte[] field=keySerializer.serialize(hfield);
        byte[] val=getSerializer().serialize(result);
        int hExpire;
        if(hashExpire < 0) {
            hExpire=result.getExpire();
        } else {
            hExpire=hashExpire;
        }
        if(hExpire == 0) {
            jedisCluster.hset(key, field, val);
        } else {
            if(hashExpireByScript) {
                List<byte[]> keys=new ArrayList<byte[]>();
                keys.add(key);

                List<byte[]> args=new ArrayList<byte[]>();
                args.add(field);
                args.add(val);
                args.add(keySerializer.serialize(String.valueOf(hExpire)));
                jedisCluster.eval(hashSetScript, keys, args);
            } else {
                jedisCluster.hset(key, field, val);
                jedisCluster.expire(key, hExpire);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(final CacheKeyTO cacheKeyTO, final Method method, final Object args[]) throws CacheCenterConnectionException {
        if(null == jedisCluster || null == cacheKeyTO) {
            return null;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return null;
        }
        CacheWrapper<Object> res=null;
        try {
            byte bytes[]=null;
            String hfield=cacheKeyTO.getHfield();
            if(null == hfield || hfield.length() == 0) {
                bytes=jedisCluster.get(keySerializer.serialize(cacheKey));
            } else {
                bytes=jedisCluster.hget(keySerializer.serialize(cacheKey), keySerializer.serialize(hfield));
            }
            Type returnType=method.getGenericReturnType();
            res=(CacheWrapper<Object>)getSerializer().deserialize(bytes, returnType);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
        }
        return res;
    }

    /**
     * 根据缓存Key删除缓存
     * @param cacheKeyTO 缓存Key
     */
    @Override
    public void delete(CacheKeyTO cacheKeyTO) throws CacheCenterConnectionException {
        if(null == jedisCluster || null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        logger.debug("delete cache:" + cacheKey);
        try {
            String hfield=cacheKeyTO.getHfield();
            if(null == hfield || hfield.length() == 0) {
                jedisCluster.del(keySerializer.serialize(cacheKey));
            } else {
                jedisCluster.hdel(keySerializer.serialize(cacheKey), keySerializer.serialize(hfield));
            }
            this.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKeyTO);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
        }
    }

    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    public int getHashExpire() {
        return hashExpire;
    }

    public void setHashExpire(int hashExpire) {
        if(hashExpire < 0) {
            return;
        }
        this.hashExpire=hashExpire;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout=timeout;
    }

    public Integer getMaxRedirections() {
        return maxRedirections;
    }

    public void setMaxRedirections(Integer maxRedirections) {
        this.maxRedirections=maxRedirections;
    }

    public String getRedisUrls() {
        return redisUrls;
    }

    public void setRedisUrls(String redisUrls) {
        this.redisUrls=redisUrls;
    }

    public GenericObjectPoolConfig getGenericObjectPoolConfig() {
        return genericObjectPoolConfig;
    }

    public void setGenericObjectPoolConfig(GenericObjectPoolConfig genericObjectPoolConfig) {
        this.genericObjectPoolConfig=genericObjectPoolConfig;
    }

}
