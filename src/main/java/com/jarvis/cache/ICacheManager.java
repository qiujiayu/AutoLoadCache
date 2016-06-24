package com.jarvis.cache;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 缓存管理
 * @author jiayu.qiu
 */
public interface ICacheManager {

    /**
     * 往缓存写数据
     * @param cacheKey 缓存Key
     * @param result 缓存数据
     * @param method Method
     */
    void setCache(CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method) throws CacheCenterConnectionException;

    /**
     * 根据缓存Key获得缓存中的数据
     * @param key 缓存key
     * @param returnType AOP拦截方法的 GenericReturnType
     * @param method Method
     * @return 缓存数据
     */
    CacheWrapper<Object> get(CacheKeyTO key, final Type returnType, Method method) throws CacheCenterConnectionException;

    /**
     * 删除缓存
     * @param key 缓存key
     */
    void delete(CacheKeyTO key) throws CacheCenterConnectionException;

    /**
     * 获取自动加载处理器
     * @return 自动加载处理器
     */
    AutoLoadHandler getAutoLoadHandler();

    /**
     * 获取表达式解析器
     * @return 表达式解析器
     */
    AbstractScriptParser getScriptParser();

    /**
     * 获取序列化工具
     * @return 序列化工具
     */
    ISerializer<Object> getSerializer();

    /**
     * 获取深度复制工具
     * @return 深度复制工具
     */
    ICloner getCloner();

    /**
     * 销毁：关闭线程
     */
    void destroy();

    /**
     * 写缓存
     * @param pjp CacheAopProxyChain
     * @param arguments arguments
     * @param cache Cache annotation
     * @param cacheKey Cache Key
     * @param cacheWrapper CacheWrapper
     * @throws Exception
     */
    void writeCache(CacheAopProxyChain pjp, Object[] arguments, Cache cache, CacheKeyTO cacheKey, CacheWrapper<Object> cacheWrapper)
        throws Exception;
}
