package com.jarvis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;

/**
 * 缓存管理抽象类
 * @author jiayu.qiu
 * @param <T> 缓存对象。
 */
public abstract class AbstractCacheManager<T> implements ICacheManager<T> {

    private static final Logger logger=Logger.getLogger(AbstractCacheManager.class);

    private final Map<String, Boolean> processing=new ConcurrentHashMap<String, Boolean>();

    private final Lock lock=new ReentrantLock();

    private AutoLoadHandler<T> autoLoadHandler;

    public AbstractCacheManager(AutoLoadConfig config) {
        autoLoadHandler=new AutoLoadHandler<T>(this, config);
    }

    @Override
    public AutoLoadHandler<T> getAutoLoadHandler() {
        return this.autoLoadHandler;
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @return
     */
    private String getCacheKey(ProceedingJoinPoint pjp, Cache cache) {
        String className=pjp.getTarget().getClass().getName();
        String methodName=pjp.getSignature().getName();
        Object[] arguments=pjp.getArgs();
        String cacheKey=null;
        if(null != cache.key() && cache.key().trim().length() > 0) {
            cacheKey=CacheUtil.getDefinedCacheKey(cache.key(), arguments);
        } else {
            cacheKey=CacheUtil.getDefaultCacheKey(className, methodName, arguments, cache.subKeySpEL());
        }
        return cacheKey;
    }

    /**
     * 处理@Cache 拦截
     * @param pjp
     * @param cache
     * @return
     * @throws Exception
     */
    public T proceed(ProceedingJoinPoint pjp, Cache cache) throws Exception {
        Object[] arguments=pjp.getArgs();
        if(!CacheUtil.isCacheable(cache, arguments)) {// 如果不进行缓存，则直接返回数据
            try {
                @SuppressWarnings("unchecked")
                T result=(T)pjp.proceed();
                return result;
            } catch(Exception e) {
                throw e;
            } catch(Throwable e) {
                throw new Exception(e);
            }
        }
        int expire=cache.expire();
        if(expire <= 0) {
            expire=300;
        }
        String cacheKey=getCacheKey(pjp, cache);
        AutoLoadTO autoLoadTO=null;
        if(CacheUtil.isAutoload(cache, arguments)) {
            try {
                autoLoadTO=autoLoadHandler.getAutoLoadTO(cacheKey);
                if(null == autoLoadTO) {
                    arguments=(Object[])BeanUtil.deepClone(arguments);
                    autoLoadTO=new AutoLoadTO(cacheKey, pjp, arguments, expire, cache.requestTimeout());
                    autoLoadHandler.setAutoLoadTO(autoLoadTO);
                }
                autoLoadTO.setLastRequestTime(System.currentTimeMillis());
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        CacheWrapper<T> cacheWrapper=this.get(cacheKey);
        if(null != cacheWrapper) {
            if(null != autoLoadTO && cacheWrapper.getLastLoadTime() > autoLoadTO.getLastLoadTime()) {
                autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime());
            }
            return cacheWrapper.getCacheObject();
        }

        Boolean isProcessing=null;
        try {
            lock.lock();
            if(null == (isProcessing=processing.get(cacheKey))) {// 为发减少数据层的并发，增加等待机制。
                processing.put(cacheKey, Boolean.TRUE);
            }
        } finally {
            lock.unlock();
        }

        if(null == isProcessing) {
            return loadData(pjp, autoLoadTO, cacheKey, expire);
        }
        long startWait=System.currentTimeMillis();
        while(System.currentTimeMillis() - startWait < 500) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch(InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            if(null == processing.get(cacheKey)) {// 防止频繁去缓存取数据，造成缓存服务器压力过大
                cacheWrapper=this.get(cacheKey);
                if(cacheWrapper != null) {
                    return cacheWrapper.getCacheObject();
                }
            }
        }
        return loadData(pjp, autoLoadTO, cacheKey, expire);
    }

    /**
     * 通过ProceedingJoinPoint加载数据
     * @param pjp
     * @param autoLoadTO
     * @param cacheKey
     * @param cacheManager
     * @param expire
     * @return
     * @throws Exception
     */
    private T loadData(ProceedingJoinPoint pjp, AutoLoadTO autoLoadTO, String cacheKey, int expire) throws Exception {
        try {
            AutoLoadConfig config=autoLoadHandler.getConfig();
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(true);
            }
            long startTime=System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            T result=(T)pjp.proceed();
            long useTime=System.currentTimeMillis() - startTime;
            if(config.isPrintSlowLog() && useTime >= config.getSlowLoadTime()) {
                String className=pjp.getTarget().getClass().getName();
                logger.error(className + "." + pjp.getSignature().getName() + ", use time:" + useTime + "ms");
            }
            CacheWrapper<T> tmp=new CacheWrapper<T>();
            tmp.setCacheObject(result);
            tmp.setLastLoadTime(System.currentTimeMillis());
            this.setCache(cacheKey, tmp, expire);
            if(null != autoLoadTO) {
                autoLoadTO.setLastLoadTime(startTime);
                autoLoadTO.addUseTotalTime(useTime);
            }
            return result;
        } catch(Exception e) {
            throw e;
        } catch(Throwable e) {
            throw new Exception(e);
        } finally {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(false);
            }
            processing.remove(cacheKey);
            synchronized(lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * 处理@CacheDelete 拦截
     * @param pjp
     * @param cacheDelete
     * @throws Exception
     */
    public void deleteCache(JoinPoint jp, CacheDelete cacheDelete) {
        Object[] arguments=jp.getArgs();
        CacheDeleteKey[] keys=cacheDelete.value();
        if(null == keys || keys.length == 0) {
            return;
        }
        for(int i=0; i < keys.length; i++) {
            CacheDeleteKey keyConfig=keys[i];
            String key=null;
            switch(keyConfig.keyType()) {
                case DEFINED:
                    key=CacheUtil.getDefinedCacheKey(keyConfig.value(), arguments);
                    break;
                case DEFAULT:
                    String className=keyConfig.cls().getName();
                    String method=keyConfig.method();
                    String subKeySpEL=keyConfig.subKeySpEL();
                    if(keyConfig.deleteByPrefixKey()) {
                        key=CacheUtil.getDefaultCacheKeyPrefix(className, method, arguments, subKeySpEL) + "*";
                    } else {
                        int len=keyConfig.argsEl().length;
                        Object[] args=new Object[len];
                        for(int j=0; j < len; j++) {
                            args[j]=CacheUtil.getElValue(keyConfig.argsEl()[j], arguments, Object.class);
                        }
                        key=CacheUtil.getDefaultCacheKey(className, method, args, subKeySpEL);
                    }
                    break;
            }
            if(null != key && key.trim().length() > 0) {
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
