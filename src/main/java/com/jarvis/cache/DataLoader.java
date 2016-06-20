package com.jarvis.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.to.ProcessingTO;

/**
 * 数据加载器
 * @author jiayu.qiu
 */
public class DataLoader {

    private static final Logger logger=Logger.getLogger(DataLoader.class);

    // 解决java.lang.NoSuchMethodError:java.util.Map.putIfAbsent
    private final ConcurrentHashMap<String, ProcessingTO> processing=new ConcurrentHashMap<String, ProcessingTO>();

    private final ICacheManager cacheManager;

    private final CacheAopProxyChain pjp;

    private final CacheKeyTO cacheKey;

    private final Cache cache;

    private final Object[] arguments;

    private final AutoLoadTO autoLoadTO;

    private boolean isFirst=true;

    private long loadDataUseTime;

    private CacheWrapper<Object> cacheWrapper;

    public DataLoader(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, CacheKeyTO cacheKey, Cache cache, ICacheManager cacheManager) {
        this.pjp=pjp;
        this.autoLoadTO=autoLoadTO;
        this.cacheKey=cacheKey;
        this.cache=cache;
        this.cacheManager=cacheManager;
        if(null == autoLoadTO) {// 用户请求
            arguments=pjp.getArgs();
        } else {// 来自AutoLoadHandler的请求
            arguments=autoLoadTO.getArgs();
        }
    }

    public DataLoader(CacheAopProxyChain pjp, CacheKeyTO cacheKey, Cache cache, ICacheManager cacheManager, Object[] arguments) {
        this.pjp=pjp;
        this.cacheKey=cacheKey;
        this.cache=cache;
        this.cacheManager=cacheManager;
        this.arguments=arguments;
        this.autoLoadTO=null;
    }

    public DataLoader(CacheAopProxyChain pjp, Cache cache, ICacheManager cacheManager) {
        this(pjp, null, null, cache, cacheManager);
    }

    public DataLoader(CacheAopProxyChain pjp, CacheKeyTO cacheKey, Cache cache, ICacheManager cacheManager) {
        this(pjp, null, cacheKey, cache, cacheManager);
    }

    public void loadData() throws Throwable {
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
        // String tname=Thread.currentThread().getName();
        if(null == isProcessing) {// 当前并发中的第一个请求
            isFirst=true;
            lock=processingTO;
            try {
                // System.out.println(tname + " first thread!");
                Object result=getData();
                buildCacheWrapper(result);
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
            isFirst=false;
            lock=isProcessing;
            long startWait=isProcessing.getStartTime();
            do {// 等待
                if(null == isProcessing) {
                    break;
                }
                if(isProcessing.isFirstFinished()) {
                    CacheWrapper<Object> _cacheWrapper=isProcessing.getCache();// 从本地缓存获取数据， 防止频繁去缓存服务器取数据，造成缓存服务器压力过大
                    // System.out.println(tname + " do FirstFinished" + " is null :" + (null == cacheWrapper));
                    if(null != _cacheWrapper) {
                        cacheWrapper=_cacheWrapper;
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
                Object result=getData();
                buildCacheWrapper(result);
            } catch(Throwable e) {
                throw e;
            } finally {
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
        }
    }

    public boolean isFirst() {
        return isFirst;
    }

    public Object getData() throws Throwable {
        try {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(true);
            }
            long loadDataStartTime=System.currentTimeMillis();
            Object result=pjp.doProxyChain(arguments);
            loadDataUseTime=System.currentTimeMillis() - loadDataStartTime;
            AutoLoadConfig config=cacheManager.getAutoLoadHandler().getConfig();
            if(config.isPrintSlowLog() && loadDataUseTime >= config.getSlowLoadTime()) {
                String className=pjp.getTargetClass().getName();
                logger.error(className + "." + pjp.getMethod().getName() + ", use time:" + loadDataUseTime + "ms");
            }
            return result;
        } catch(Throwable e) {
            throw e;
        } finally {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(false);
            }
        }
    }

    public void buildCacheWrapper(Object result) {
        int expire=cache.expire();
        try {
            expire=cacheManager.getScriptParser().getRealExpire(cache.expire(), cache.expireExpression(), arguments, result);
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        cacheWrapper=new CacheWrapper<Object>(result, expire);
    }

    public CacheWrapper<Object> getCacheWrapper() {
        if(null == cacheWrapper) {
            throw new RuntimeException("run loadData() or buildCacheWrapper() please!");
        }
        return cacheWrapper;
    }

    public long getLoadDataUseTime() {
        return loadDataUseTime;
    }

}
