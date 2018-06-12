package com.jarvis.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 异步刷新缓存处理器
 * 
 * @author: jiayu.qiu
 */
@Slf4j
public class RefreshHandler {

    private static final int REFRESH_MIN_EXPIRE = 120;

    private static final int ONE_THOUSAND_MS = 1000;
    /**
     * 刷新缓存线程池
     */
    private final ThreadPoolExecutor refreshThreadPool;

    /**
     * 正在刷新缓存队列
     */
    private final ConcurrentHashMap<CacheKeyTO, Byte> refreshing;

    private final CacheHandler cacheHandler;

    public RefreshHandler(CacheHandler cacheHandler, AutoLoadConfig config) {
        this.cacheHandler = cacheHandler;
        int corePoolSize = config.getRefreshThreadPoolSize();// 线程池的基本大小
        int maximumPoolSize = config.getRefreshThreadPoolMaxSize();// 线程池最大大小,线程池允许创建的最大线程数。如果队列满了，并且已创建的线程数小于最大线程数，则线程池会再创建新的线程执行任务。值得注意的是如果使用了无界的任务队列这个参数就没什么效果。
        int keepAliveTime = config.getRefreshThreadPoolkeepAliveTime();
        TimeUnit unit = TimeUnit.MINUTES;
        int queueCapacity = config.getRefreshQueueCapacity();// 队列容量
        refreshing = new ConcurrentHashMap<CacheKeyTO, Byte>(queueCapacity);
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueCapacity);
        RejectedExecutionHandler rejectedHandler = new RefreshRejectedExecutionHandler();
        refreshThreadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue,
                new ThreadFactory() {

                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    private final String namePrefix = "autoload-cache-RefreshHandler-";

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }, rejectedHandler);
    }

    public void removeTask(CacheKeyTO cacheKey) {
        refreshing.remove(cacheKey);
    }

    public void doRefresh(CacheAopProxyChain pjp, Cache cache, CacheKeyTO cacheKey, CacheWrapper<Object> cacheWrapper) {
        int expire = cacheWrapper.getExpire();
        if (expire < REFRESH_MIN_EXPIRE) {// 如果过期时间太小了，就不允许自动加载，避免加载过于频繁，影响系统稳定性
            return;
        }
        // 计算超时时间
        int alarmTime = cache.alarmTime();
        long timeout;
        if (alarmTime > 0 && alarmTime < expire) {
            timeout = expire - alarmTime;
        } else {
            if (expire >= 600) {
                timeout = expire - REFRESH_MIN_EXPIRE;
            } else {
                timeout = expire - 60;
            }
        }

        if ((System.currentTimeMillis() - cacheWrapper.getLastLoadTime()) < (timeout * ONE_THOUSAND_MS)) {
            return;
        }
        Byte tmpByte = refreshing.get(cacheKey);
        // 如果有正在刷新的请求，则不处理
        if (null != tmpByte) {
            return;
        }
        tmpByte = 1;
        if (null == refreshing.putIfAbsent(cacheKey, tmpByte)) {
            try {
                refreshThreadPool.execute(new RefreshTask(pjp, cache, cacheKey, cacheWrapper));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void shutdown() {
        refreshThreadPool.shutdownNow();
        try {
            refreshThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class RefreshTask implements Runnable {

        private final CacheAopProxyChain pjp;

        private final Cache cache;

        private final CacheKeyTO cacheKey;

        private final CacheWrapper<Object> cacheWrapper;

        private final Object[] arguments;

        public RefreshTask(CacheAopProxyChain pjp, Cache cache, CacheKeyTO cacheKey, CacheWrapper<Object> cacheWrapper)
                throws Exception {
            this.pjp = pjp;
            this.cache = cache;
            this.cacheKey = cacheKey;
            this.cacheWrapper = cacheWrapper;
            if (cache.argumentsDeepcloneEnable()) {
                // 进行深度复制(因为是异步执行，防止外部修改参数值)
                this.arguments = (Object[]) cacheHandler.getCloner().deepCloneMethodArgs(pjp.getMethod(),
                        pjp.getArgs());
            } else {
                this.arguments = pjp.getArgs();
            }
        }

        @Override
        public void run() {
            DataLoaderFactory factory = DataLoaderFactory.getInstance();
            DataLoader dataLoader = factory.getDataLoader();
            CacheWrapper<Object> newCacheWrapper = null;
            boolean isFirst = false;
            try {
                newCacheWrapper = dataLoader.init(pjp, cacheKey, cache, cacheHandler, arguments).loadData()
                        .getCacheWrapper();
                isFirst = dataLoader.isFirst();
            } catch (Throwable ex) {
                log.error(ex.getMessage(), ex);
            } finally {
                factory.returnObject(dataLoader);
            }
            if (isFirst) {
                // 如果数据加载失败，则把旧数据进行续租
                if (null == newCacheWrapper && null != cacheWrapper) {
                    int newExpire = cacheWrapper.getExpire() / 2;
                    if (newExpire < 120) {
                        newExpire = 120;
                    }
                    newCacheWrapper = new CacheWrapper<Object>(cacheWrapper.getCacheObject(), newExpire);
                }
                try {
                    if (null != newCacheWrapper) {
                        cacheHandler.writeCache(pjp, arguments, cache, cacheKey, newCacheWrapper);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
            refreshing.remove(cacheKey);
        }

        public CacheKeyTO getCacheKey() {
            return cacheKey;
        }

    }

    class RefreshRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                Runnable last = e.getQueue().poll();
                if (last instanceof RefreshTask) {
                    RefreshTask lastTask = (RefreshTask) last;
                    refreshing.remove(lastTask.getCacheKey());
                }
                e.execute(r);
            }
        }

    }
}
