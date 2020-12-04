package com.jarvis.cache;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.exception.LoadDataTimeOutException;
import com.jarvis.cache.lock.ILock;
import com.jarvis.cache.to.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据加载器
 *
 *
 */
@Slf4j
public class DataLoader {

    private CacheHandler cacheHandler;

    private CacheAopProxyChain pjp;

    private CacheKeyTO cacheKey;

    private Cache cache;

    private Object[] arguments;

    private AutoLoadTO autoLoadTO;

    private boolean isFirst;

    private long loadDataUseTime;

    private CacheWrapper<Object> cacheWrapper;

    private int tryCnt = 0;

    public DataLoader() {
    }

    public DataLoader init(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, CacheKeyTO cacheKey, Cache cache, CacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
        this.pjp = pjp;
        this.cacheKey = cacheKey;
        this.cache = cache;
        this.autoLoadTO = autoLoadTO;

        if (null == autoLoadTO) {
            // 用户请求
            this.arguments = pjp.getArgs();
        } else {
            // 来自AutoLoadHandler的请求
            this.arguments = autoLoadTO.getArgs();
        }
        this.loadDataUseTime = 0;
        this.tryCnt = 0;
        return this;
    }

    public DataLoader init(CacheAopProxyChain pjp, CacheKeyTO cacheKey, Cache cache, CacheHandler cacheHandler, Object[] arguments) {
        this.cacheHandler = cacheHandler;
        this.pjp = pjp;
        this.cacheKey = cacheKey;
        this.cache = cache;
        this.autoLoadTO = null;
        this.arguments = arguments;
        this.loadDataUseTime = 0;
        this.tryCnt = 0;
        return this;
    }

    public DataLoader init(CacheAopProxyChain pjp, Cache cache, CacheHandler cacheHandler) {
        return init(pjp, null, null, cache, cacheHandler);
    }

    public DataLoader init(CacheAopProxyChain pjp, CacheKeyTO cacheKey, Cache cache, CacheHandler cacheHandler) {
        return init(pjp, null, cacheKey, cache, cacheHandler);
    }

    /**
     * 重置数据，避免长期缓存，去除引用关系，好让GC回收引用对象
     */
    public void reset() {
        this.cacheHandler = null;
        this.pjp = null;
        this.cacheKey = null;
        this.cache = null;
        this.autoLoadTO = null;
        this.arguments = null;
        this.cacheWrapper = null;
    }

    public DataLoader loadData() throws Throwable {
        ProcessingTO processing = cacheHandler.processing.get(cacheKey);
        if (null == processing) {
            ProcessingTO newProcessing = new ProcessingTO();
            // 为发减少数据层的并发，增加等待机制。
            ProcessingTO firstProcessing = cacheHandler.processing.putIfAbsent(cacheKey, newProcessing);
            // 当前并发中的第一个请求
            if (null == firstProcessing) {
                isFirst = true;
                processing = newProcessing;
            } else {
                isFirst = false;
                // 获取到第一个线程的ProcessingTO 的引用，保证所有请求都指向同一个引用
                processing = firstProcessing;
            }
        } else {
            isFirst = false;
        }
        Object lock = processing;
        String threadName = Thread.currentThread().getName();
        if (isFirst) {
            if (log.isTraceEnabled()) {
                log.trace("{} first thread!", threadName);
            }
            try {
                doFirstRequest(processing);
            } catch (Throwable e) {
                processing.setError(e);
                throw e;
            } finally {
                processing.setFirstFinished(true);
                cacheHandler.processing.remove(cacheKey);
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        } else {
            doWaitRequest(processing, lock);
        }
        return this;
    }

    private int tryLock(ILock distributedLock, String lockKey) {
        try {
            return distributedLock.tryLock(lockKey, cache.lockExpire()) ? 1 : 0;
        } catch (Throwable e) {
            if (cache.openLockDown()) {
                // 关闭分布式锁
                cacheHandler.setLock(null);
                log.error("分布式锁异常，强制停止使用分布式锁!", e);
            } else {
                log.error("分布式锁异常!", e);
            }
        }
        return 2;
    }

    private void doFirstRequest(ProcessingTO processingTO) throws Throwable {
        ILock distributedLock = cacheHandler.getLock();
        if (null != distributedLock && cache.lockExpire() > 0) {
            // 开启分布式锁
            String lockKey = cacheKey.getLockKey();
            long startWait = processingTO.getStartTime();
            // 获得分布式锁
            int lockState = tryLock(distributedLock, lockKey);
            if (lockState == 1) {
                try {
                    getData();
                } finally {
                    try {
                        distributedLock.unlock(lockKey);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } else if (lockState == 0) {
                do {
                    int tryCnt = 20;
                    // 没有获得锁时，定时缓存尝试获取数据
                    for (int i = 0; i < tryCnt; i++) {
                        cacheWrapper = cacheHandler.get(cacheKey, pjp.getMethod());
                        if (null != cacheWrapper) {
                            break;
                        }
                        Thread.sleep(10);
                    }
                    if (null != cacheWrapper) {
                        break;
                    }
                } while (System.currentTimeMillis() - startWait < cache.waitTimeOut());
            }
        }
        if (null == cacheWrapper) {
            getData();
        }
        // 本地缓存
        processingTO.setCache(cacheWrapper);
    }

    private void doWaitRequest(ProcessingTO processing, Object lock) throws Throwable {
        long startWait = processing.getStartTime();
        String tname = Thread.currentThread().getName();
        do {
            // 等待
            if (processing.isFirstFinished()) {
                // 从本地内存获取数据，防止频繁去缓存服务器取数据，造成缓存服务器压力过大
                CacheWrapper<Object> tmpcacheWrapper = processing.getCache();
                if (log.isTraceEnabled()) {
                    log.trace("{} do FirstFinished" + " is null :{}", tname, (null == tmpcacheWrapper));
                }
                if (null != tmpcacheWrapper) {
                    cacheWrapper = tmpcacheWrapper;
                    return;
                }
                Throwable error = processing.getError();
                // 当DAO出错时，直接抛异常
                if (null != error) {
                    if (log.isTraceEnabled()) {
                        log.trace("{} do error", tname);
                    }
                    throw error;
                }
                break;
            } else {
                synchronized (lock) {
                    if (log.isTraceEnabled()) {
                        log.trace("{} do wait", tname);
                    }
                    try {
                        // 如果要测试lock对象是否有效，wait时间去掉就可以
                        lock.wait(2);
                    } catch (InterruptedException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            }
        } while (System.currentTimeMillis() - startWait < cache.waitTimeOut());
        if (null == cacheWrapper) {
            cacheWrapper = cacheHandler.get(cacheKey, pjp.getMethod());
        }
        if (null == cacheWrapper) {
            AutoLoadConfig config = cacheHandler.getAutoLoadConfig();
            if (tryCnt < config.getLoadDataTryCnt()) {
                tryCnt++;
                loadData();
            } else {
                throw new LoadDataTimeOutException("cache for key \"" + cacheKey.getCacheKey() + "\" loaded " + tryCnt + " times.");
            }
        }
    }

    public boolean isFirst() {
        return isFirst;
    }

    public DataLoader getData() throws Throwable {
        try {
            if (null != autoLoadTO) {
                autoLoadTO.setLoading(true);
            }
            long loadDataStartTime = System.currentTimeMillis();
            Object result = pjp.doProxyChain(arguments);
            loadDataUseTime = System.currentTimeMillis() - loadDataStartTime;
            AutoLoadConfig config = cacheHandler.getAutoLoadConfig();
            String className = pjp.getMethod().getDeclaringClass().getName();
            if (config.isPrintSlowLog() && loadDataUseTime >= config.getSlowLoadTime()) {
                log.warn("{}.{}, use time:{}ms", className, pjp.getMethod().getName(), loadDataUseTime);
            }
            if (log.isDebugEnabled()) {
                log.debug("{}.{}, result is null : {}", className, pjp.getMethod().getName(), null == result);
            }
            buildCacheWrapper(result);
        } catch (Throwable e) {
            throw e;
        } finally {
            if (null != autoLoadTO) {
                autoLoadTO.setLoading(false);
            }
        }
        return this;
    }

    private void buildCacheWrapper(Object result) {
        int expire = cache.expire();
        try {
            expire = cacheHandler.getScriptParser().getRealExpire(cache.expire(), cache.expireExpression(), arguments,
                    result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        cacheWrapper = new CacheWrapper<>(result, expire);
    }

    public CacheWrapper<Object> getCacheWrapper() {
        if (null == cacheWrapper) {
            throw new RuntimeException("run loadData() or buildCacheWrapper() please!");
        }
        return cacheWrapper;
    }

    public long getLoadDataUseTime() {
        return loadDataUseTime;
    }

}
