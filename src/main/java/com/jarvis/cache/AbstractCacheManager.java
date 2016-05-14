package com.jarvis.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.annotation.ExCache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.aop.DeleteCacheAopProxyChain;
import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.to.ProcessingTO;
import com.jarvis.cache.type.CacheOpType;

/**
 * 缓存管理抽象类
 * @author jiayu.qiu
 */
public abstract class AbstractCacheManager implements ICacheManager {

    private static final Logger logger=Logger.getLogger(AbstractCacheManager.class);

    // 解决java.lang.NoSuchMethodError:java.util.Map.putIfAbsent
    private final ConcurrentHashMap<String, ProcessingTO> processing=new ConcurrentHashMap<String, ProcessingTO>();

    private AutoLoadHandler autoLoadHandler;

    private String namespace;

    /**
     * 序列化工具，默认使用Hessian2
     */
    private ISerializer<Object> serializer=new HessianSerializer();

    public AbstractCacheManager(AutoLoadConfig config) {
        autoLoadHandler=new AutoLoadHandler(this, config);
    }

    public ISerializer<Object> getSerializer() {
        return serializer;
    }

    public void setSerializer(ISerializer<Object> serializer) {
        this.serializer=serializer;
    }

    @Override
    public AutoLoadHandler getAutoLoadHandler() {
        return this.autoLoadHandler;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace=namespace;
    }

    /**
     * 生成缓存KeyTO
     * @param className 类名
     * @param methodName 方法名
     * @param arguments 参数
     * @param _key key
     * @param _hfield hfield
     * @param result 执行实际方法的返回值
     * @return CacheKeyTO
     */
    private CacheKeyTO getCacheKey(String className, String methodName, Object[] arguments, String _key, String _hfield,
        Object result, boolean hasRetVal) {
        String key=null;
        String hfield=null;
        if(null != _key && _key.trim().length() > 0) {
            key=CacheUtil.getDefinedCacheKey(_key, arguments, result, hasRetVal);
            if(null != _hfield && _hfield.trim().length() > 0) {
                hfield=CacheUtil.getDefinedCacheKey(_hfield, arguments, result, hasRetVal);
            }
        } else {
            key=CacheUtil.getDefaultCacheKey(className, methodName, arguments);
        }
        if(null == key || key.trim().length() == 0) {
            logger.error(className + "." + methodName + "; cache key is empty");
            return null;
        }
        CacheKeyTO to=new CacheKeyTO();
        to.setNamespace(namespace);
        to.setKey(key);
        to.setHfield(hfield);
        return to;
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @return String 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache) {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        Object[] arguments=pjp.getArgs();
        String _key=cache.key();
        String _hfield=cache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, null, false);
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @param result 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache, Object result) {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        Object[] arguments=pjp.getArgs();
        String _key=cache.key();
        String _hfield=cache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, result, true);
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @param result 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, ExCache cache, Object result) {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        Object[] arguments=pjp.getArgs();
        if(null != autoLoadTO) {
            arguments=autoLoadTO.getArgs();
        }
        String _key=cache.key();
        if(null == _key || _key.trim().length() == 0) {
            return null;
        }
        String _hfield=cache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, result, true);
    }

    /**
     * 生成缓存 Key
     * @param jp
     * @param cacheDeleteKey
     * @param retVal 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(DeleteCacheAopProxyChain jp, CacheDeleteKey cacheDeleteKey, Object retVal) {
        String className=jp.getTargetClass().getName();
        String methodName=jp.getMethod().getName();
        Object[] arguments=jp.getArgs();
        String _key=cacheDeleteKey.value();
        String _hfield=cacheDeleteKey.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, retVal, true);

    }

    /**
     * 处理@Cache 拦截
     * @param pjp 切面
     * @param cache 注解
     * @return T 返回值
     * @throws Exception 异常
     */
    public Object proceed(CacheAopProxyChain pjp, Cache cache) throws Throwable {
        Object[] arguments=pjp.getArgs();
        // Signature signature=pjp.getSignature();
        // MethodSignature methodSignature=(MethodSignature)signature;
        // Class returnType=methodSignature.getReturnType(); // 获取返回值类型
        // System.out.println("returnType:" + returnType.getName());
        if(null != cache.opType() && cache.opType() == CacheOpType.WRITE) {// 更新缓存操作
            Object result=getData(pjp, null);
            if(CacheUtil.isCacheable(cache, arguments, result)) {
                CacheKeyTO cacheKey=getCacheKey(pjp, cache, result);
                writeCache(pjp, null, cache, cacheKey, result);
            }
            return result;
        }
        if(!CacheUtil.isCacheable(cache, arguments)) {// 如果不进行缓存，则直接返回数据
            return getData(pjp, null);
        }
        CacheKeyTO cacheKey=getCacheKey(pjp, cache);
        if(null == cacheKey) {
            return getData(pjp, null);
        }
        AutoLoadTO autoLoadTO=null;
        if(CacheUtil.isAutoload(cache, arguments)) {
            autoLoadTO=autoLoadHandler.getAutoLoadTO(cacheKey);
            if(null == autoLoadTO) {
                AutoLoadTO tmp=autoLoadHandler.putIfAbsent(cacheKey, pjp, cache, serializer);
                if(null != tmp) {
                    autoLoadTO=tmp;
                }
            }
            autoLoadTO.setLastRequestTime(System.currentTimeMillis());
        }
        CacheWrapper cacheWrapper=this.get(cacheKey);// 从缓存中获取数据
        if(null != cacheWrapper && !cacheWrapper.isExpired()) {
            if(null != autoLoadTO && cacheWrapper.getLastLoadTime() > autoLoadTO.getLastLoadTime()) {// 同步最后加载时间
                autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime());
            }
            return cacheWrapper.getCacheObject();
        }
        return loadData(pjp, autoLoadTO, cacheKey, cache);// 从DAO加载数据
    }

    /**
     * 写缓存
     * @param pjp CacheAopProxyChain
     * @param autoLoadTO AutoLoadTO
     * @param cache Cache annotation
     * @param cacheKey Cache Key
     * @param result cache data
     * @return CacheWrapper
     */
    private CacheWrapper writeCache(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, Cache cache, CacheKeyTO cacheKey, Object result) {
        if(null == cacheKey) {
            return null;
        }
        Object[] arguments=pjp.getArgs();
        if(null != autoLoadTO) {
            arguments=autoLoadTO.getArgs();
        }
        String expireExpression=cache.expireExpression();
        Integer tmpExpire=null;
        if(null != expireExpression && expireExpression.length() > 0) {
            try {
                tmpExpire=CacheUtil.getElValue(expireExpression, arguments, result, true, Integer.class);
            } catch(Exception ex) {

            }
        }

        int expire=cache.expire();
        if(null != tmpExpire && tmpExpire.intValue() >= 0) {
            expire=tmpExpire.intValue();
        }
        CacheWrapper cacheWrapper=new CacheWrapper(result, expire);
        this.setCache(cacheKey, cacheWrapper);

        ExCache[] exCaches=cache.exCache();
        if(null != exCaches && exCaches.length > 0) {

            for(ExCache exCache: exCaches) {
                if(!CacheUtil.isCacheable(exCache, arguments, result)) {
                    continue;
                }
                CacheKeyTO exCacheKey=getCacheKey(pjp, autoLoadTO, exCache, result);
                if(null == exCacheKey) {
                    continue;
                }
                Object exResult=null;
                if(null == exCache.cacheObject() || exCache.cacheObject().length() == 0) {
                    exResult=result;
                } else {
                    exResult=CacheUtil.getElValue(exCache.cacheObject(), arguments, result, true, Object.class);
                }
                AutoLoadTO tmpAutoLoadTO=this.autoLoadHandler.getAutoLoadTO(exCacheKey);
                if(null != tmpAutoLoadTO) {
                    tmpAutoLoadTO.setExpire(exCache.expire());
                }
                CacheWrapper exCacheWrapper=new CacheWrapper(exResult, exCache.expire());
                this.setCache(exCacheKey, exCacheWrapper);
            }
        }
        return cacheWrapper;
    }

    /**
     * 通过CacheAopProxyChain加载数据
     * @param pjp
     * @param autoLoadTO
     * @param cacheKey
     * @param cache
     * @return 返回值
     * @throws Exception
     */
    @Override
    public Object loadData(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, CacheKeyTO cacheKey, Cache cache) throws Throwable {
        String fullKey=cacheKey.getFullKey();
        ProcessingTO isProcessing=processing.get(fullKey);
        ProcessingTO processingTO=null;
        if(null == isProcessing) {
            processingTO=new ProcessingTO();
            ProcessingTO _isProcessing=processing.putIfAbsent(fullKey, processingTO);// 为发减少数据层的并发，增加等待机制。
            if(null != _isProcessing) {
                isProcessing=_isProcessing;// 获取到第一个线程的ProcessingTO 的引用，保证所有请求都指向同一个引用
            }
        }
        Object lock=null;
        Object result=null;
        // String tname=Thread.currentThread().getName();
        if(null == isProcessing) {
            lock=processingTO;
            try {
                // System.out.println(tname + " first thread!");
                result=getData(pjp, autoLoadTO);
                CacheWrapper cacheWrapper=writeCache(pjp, autoLoadTO, cache, cacheKey, result);
                processingTO.setCache(cacheWrapper);// 本地缓存
            } catch(Throwable e) {
                processingTO.setError(e);
                throw e;
            } finally {
                processingTO.setFirstFinished(true);
                processing.remove(fullKey);
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
        } else {
            lock=isProcessing;
            long startWait=isProcessing.getStartTime();
            CacheWrapper cacheWrapper=null;
            do {// 等待
                if(null == isProcessing) {
                    break;
                }
                if(isProcessing.isFirstFinished()) {
                    cacheWrapper=isProcessing.getCache();// 从本地缓存获取数据， 防止频繁去缓存服务器取数据，造成缓存服务器压力过大
                    // System.out.println(tname + " do FirstFinished" + " is null :" + (null == cacheWrapper));
                    if(null != cacheWrapper) {
                        return cacheWrapper.getCacheObject();
                    }
                    Throwable error=isProcessing.getError();
                    if(null != error) {// 当DAO出错时，直接抛异常
                        // System.out.println(tname + " do error");
                        throw error;
                    }
                    break;
                } else {
                    synchronized(lock) {
                        // System.out.println(tname + " do wait");
                        try {
                            lock.wait(50);// 如果要测试lock对象是否有效，wait时间去掉就可以
                        } catch(InterruptedException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                }
            } while(System.currentTimeMillis() - startWait < cache.waitTimeOut());
            try {
                result=getData(pjp, autoLoadTO);
                writeCache(pjp, autoLoadTO, cache, cacheKey, result);
            } catch(Throwable e) {
                throw e;
            } finally {
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
        }

        return result;
    }

    private Object getData(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO) throws Throwable {
        try {
            long startTime=System.currentTimeMillis();
            Object[] arguments;
            if(null == autoLoadTO) {
                arguments=pjp.getArgs();
            } else {
                arguments=autoLoadTO.getArgs();
            }
            Object result=pjp.doProxyChain(arguments);
            long useTime=System.currentTimeMillis() - startTime;
            AutoLoadConfig config=autoLoadHandler.getConfig();
            if(config.isPrintSlowLog() && useTime >= config.getSlowLoadTime()) {
                String className=pjp.getTargetClass().getName();
                logger.error(className + "." + pjp.getMethod().getName() + ", use time:" + useTime + "ms");
            }
            if(null != autoLoadTO) {
                autoLoadTO.setLastLoadTime(startTime);
                autoLoadTO.addUseTotalTime(useTime);
            }
            return result;
        } catch(Throwable e) {
            throw e;
        }
    }

    /**
     * 处理@CacheDelete 拦截
     * @param jp 切点
     * @param cacheDelete 拦截到的注解
     * @param retVal 返回值
     */
    public void deleteCache(DeleteCacheAopProxyChain jp, CacheDelete cacheDelete, Object retVal) {
        Object[] arguments=jp.getArgs();
        CacheDeleteKey[] keys=cacheDelete.value();
        if(null == keys || keys.length == 0) {
            return;
        }
        for(int i=0; i < keys.length; i++) {
            CacheDeleteKey keyConfig=keys[i];
            if(!CacheUtil.isCanDelete(keyConfig, arguments, retVal)) {
                continue;
            }
            CacheKeyTO key=getCacheKey(jp, keyConfig, retVal);
            if(null != key) {
                this.delete(key);
            }
        }
    }

    @Override
    public void destroy() {
        autoLoadHandler.shutdown();
        autoLoadHandler=null;
        logger.info("cache destroy ... ... ...");
    }
}
