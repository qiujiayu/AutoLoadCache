package com.jarvis.cache.script;

import java.lang.reflect.Method;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.annotation.ExCache;
import com.jarvis.cache.type.CacheOpType;

/**
 * 表达式处理
 * 
 * @author jiayu.qiu
 */
public abstract class AbstractScriptParser {

    protected static final String TARGET = "target";

    protected static final String ARGS = "args";

    protected static final String RET_VAL = "retVal";

    protected static final String HASH = "hash";

    protected static final String EMPTY = "empty";

    /**
     * 为了简化表达式，方便调用Java static 函数，在这里注入表达式自定义函数
     * 
     * @param name 自定义函数名
     * @param method 调用的方法
     */
    public abstract void addFunction(String name, Method method);

    /**
     * 将表达式转换期望的值
     * 
     * @param exp 生成缓存Key的表达式
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @param retVal 结果值（缓存数据）
     * @param hasRetVal 是否使用 retVal 参数
     * @param valueType 表达式最终返回值类型
     * @return T value 返回值
     * @param <T> 泛型
     * @throws Exception 异常
     */
    public abstract <T> T getElValue(String exp, Object target, Object[] arguments, Object retVal, boolean hasRetVal,
            Class<T> valueType) throws Exception;

    /**
     * 将表达式转换期望的值
     * 
     * @param keyEL 生成缓存Key的表达式
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @param valueType 值类型
     * @return T Value 返回值
     * @param <T> 泛型
     * @throws Exception 异常
     */
    public <T> T getElValue(String keyEL, Object target, Object[] arguments, Class<T> valueType) throws Exception {
        return this.getElValue(keyEL, target, arguments, null, false, valueType);
    }

    /**
     * 根据请求参数和执行结果值，进行构造缓存Key
     * 
     * @param keyEL 生成缓存Key的表达式
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @param retVal 结果值
     * @param hasRetVal 是否有retVal
     * @return CacheKey 缓存Key
     * @throws Exception 异常
     */
    public String getDefinedCacheKey(String keyEL, Object target, Object[] arguments, Object retVal, boolean hasRetVal)
            throws Exception {
        return this.getElValue(keyEL, target, arguments, retVal, hasRetVal, String.class);
    }

    /**
     * 是否可以缓存
     * 
     * @param cache Cache
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @return cacheAble 是否可以进行缓存
     * @throws Exception 异常
     */
    public boolean isCacheable(Cache cache, Object target, Object[] arguments) throws Exception {
        boolean rv = true;
        if (null != arguments && arguments.length > 0 && null != cache.condition() && cache.condition().length() > 0) {
            rv = getElValue(cache.condition(), target, arguments, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以缓存
     * 
     * @param cache Cache
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @param result 执行结果
     * @return cacheAble 是否可以进行缓存
     * @throws Exception 异常
     */
    public boolean isCacheable(Cache cache, Object target, Object[] arguments, Object result) throws Exception {
        boolean rv = true;
        if (null != cache.condition() && cache.condition().length() > 0) {
            rv = this.getElValue(cache.condition(), target, arguments, result, true, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以缓存
     * 
     * @param cache ExCache
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @param result 执行结果
     * @return cacheAble 是否可以进行缓存
     * @throws Exception 异常
     */
    public boolean isCacheable(ExCache cache, Object target, Object[] arguments, Object result) throws Exception {
        if (null == cache || cache.expire() < 0 || cache.key().length() == 0) {
            return false;
        }
        boolean rv = true;
        if (null != cache.condition() && cache.condition().length() > 0) {
            rv = this.getElValue(cache.condition(), target, arguments, result, true, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以自动加载
     * 
     * @param cache Cache 注解
     * @param target AOP 拦截到的实例
     * @param arguments 参数
     * @param retVal return value
     * @return autoload 是否自动加载
     * @throws Exception 异常
     */
    public boolean isAutoload(Cache cache, Object target, Object[] arguments, Object retVal) throws Exception {
        if (cache.opType() == CacheOpType.WRITE) {
            return false;
        }
        boolean autoload = cache.autoload();
        if (null != arguments && arguments.length > 0 && null != cache.autoloadCondition()
                && cache.autoloadCondition().length() > 0) {
            autoload = this.getElValue(cache.autoloadCondition(), target, arguments, retVal, true, Boolean.class);
        }
        return autoload;
    }

    /**
     * 是否可以删除缓存
     * 
     * @param cacheDeleteKey CacheDeleteKey注解
     * @param arguments 参数
     * @param retVal 结果值
     * @return Can Delete
     * @throws Exception 异常
     */
    public boolean isCanDelete(CacheDeleteKey cacheDeleteKey, Object[] arguments, Object retVal) throws Exception {
        boolean rv = true;
        if (null != arguments && arguments.length > 0 && null != cacheDeleteKey.condition()
                && cacheDeleteKey.condition().length() > 0) {
            rv = this.getElValue(cacheDeleteKey.condition(), null, arguments, retVal, true, Boolean.class);
        }
        return rv;
    }

    /**
     * 获取真实的缓存时间值
     * 
     * @param expire 缓存时间
     * @param expireExpression 缓存时间表达式
     * @param arguments 方法参数
     * @param result 方法执行返回结果
     * @return real expire
     * @throws Exception 异常
     */
    public int getRealExpire(int expire, String expireExpression, Object[] arguments, Object result) throws Exception {
        Integer tmpExpire = null;
        if (null != expireExpression && expireExpression.length() > 0) {
            tmpExpire = this.getElValue(expireExpression, null, arguments, result, true, Integer.class);
            if (null != tmpExpire && tmpExpire.intValue() >= 0) {
                // 返回缓存时间表达式计算的时间
                return tmpExpire.intValue();
            }
        }
        return expire;
    }
}
