package com.jarvis.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

public class RefreshHandler {

    private static final Logger logger=Logger.getLogger(RefreshHandler.class);

    /**
     * 刷新缓存线程池
     */
    private final ThreadPoolExecutor refreshThreadPool;

    /**
     * 正在刷新缓存队列
     */
    private final ConcurrentHashMap<String, Byte> refreshing;

    private final ICacheManager cacheManager;

    public RefreshHandler(ICacheManager cacheManager) {
        this.cacheManager=cacheManager;
        int corePoolSize=2;// 线程池的基本大小
        int maximumPoolSize=20;// 线程池最大大小,线程池允许创建的最大线程数。如果队列满了，并且已创建的线程数小于最大线程数，则线程池会再创建新的线程执行任务。值得注意的是如果使用了无界的任务队列这个参数就没什么效果。
        int keepAliveTime=10;
        TimeUnit unit=TimeUnit.MINUTES;
        int queueCapacity=2000;// 队列容量
        refreshing=new ConcurrentHashMap<String, Byte>(queueCapacity);
        LinkedBlockingQueue<Runnable> queue=new LinkedBlockingQueue<Runnable>(queueCapacity);
        RejectedExecutionHandler rejectedHandler=new RefreshRejectedExecutionHandler();
        refreshThreadPool=new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, rejectedHandler);
    }

    public void doRefresh(CacheAopProxyChain pjp, Cache cache, CacheKeyTO cacheKey, CacheWrapper<Object> cacheWrapper) {
        int expire=cacheWrapper.getExpire();
        if(expire < 60) {// 如果过期时间太小了，就不允许自动加载，避免加载过于频繁，影响系统稳定性
            return;
        }
        // 计算超时时间
        int alarmTime=cache.alarmTime();
        long timeout;
        if(alarmTime > 0 && alarmTime < expire) {
            timeout=expire - alarmTime;
        } else {
            if(expire >= 600) {
                timeout=expire - 120;
            } else {
                timeout=expire - 60;
            }
        }
        if((System.currentTimeMillis() - cacheWrapper.getLastLoadTime()) < (timeout * 1000)) {
            return;
        }
        String fullKey=cacheKey.getFullKey();
        Byte tmpByte=refreshing.get(fullKey);
        if(null != tmpByte) {// 如果有正在刷新的请求，则不处理
            return;
        }
        tmpByte=1;
        if(null == refreshing.putIfAbsent(fullKey, tmpByte)) {
            try {
                refreshThreadPool.execute(new RefreshTask(pjp, cache, cacheKey, cacheWrapper));
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void shutdown() {
        refreshThreadPool.shutdownNow();
    }

    class RefreshTask implements Runnable {

        private final CacheAopProxyChain pjp;

        private final Cache cache;

        private final CacheKeyTO cacheKey;

        private final CacheWrapper<Object> cacheWrapper;

        private final Object[] arguments;

        public RefreshTask(CacheAopProxyChain pjp, Cache cache, CacheKeyTO cacheKey, CacheWrapper<Object> cacheWrapper)
            throws Exception {
            this.pjp=pjp;
            this.cache=cache;
            this.cacheKey=cacheKey;
            this.cacheWrapper=cacheWrapper;
            this.arguments=(Object[])cacheManager.getSerializer().deepClone(pjp.getArgs()); // 进行深度复制(因为是异步执行，防止外部修改参数值)
        }

        @Override
        public void run() {
            DataLoader dataLoader=new DataLoader(pjp, cacheKey, cache, cacheManager, arguments);
            CacheWrapper<Object> newCacheWrapper=null;
            try {
                dataLoader.loadData();// 从DAO加载数据
                newCacheWrapper=dataLoader.getCacheWrapper();
            } catch(Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
            if(dataLoader.isFirst() || null == newCacheWrapper) {
                if(null == newCacheWrapper && null != cacheWrapper) {// 如果加载失败，则把旧数据进行续租
                    int newExpire=cacheWrapper.getExpire() / 2;
                    newCacheWrapper=new CacheWrapper<Object>(cacheWrapper.getCacheObject(), newExpire);
                }
                try {
                    if(null != newCacheWrapper) {
                        cacheManager.writeCache(pjp, arguments, cache, cacheKey, newCacheWrapper);
                    }
                } catch(Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            String fullKey=cacheKey.getFullKey();
            refreshing.remove(fullKey);
        }

        public CacheKeyTO getCacheKey() {
            return cacheKey;
        }

    }

    class RefreshRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if(!e.isShutdown()) {
                Runnable last=e.getQueue().poll();
                if(last instanceof RefreshTask) {
                    RefreshTask lastTask=(RefreshTask)last;
                    String fullKey=lastTask.getCacheKey().getFullKey();
                    refreshing.remove(fullKey);
                }
                e.execute(r);
            }
        }

    }
}
