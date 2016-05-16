package com.jarvis.cache;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.BeanUtil;

/**
 * 用于处理自动加载缓存，sortThread 从autoLoadMap中取出数据，然后通知threads进行处理。
 * @author jiayu.qiu
 */
public class AutoLoadHandler {

    private static final Logger logger=Logger.getLogger(AutoLoadHandler.class);

    public static final Integer AUTO_LOAD_MIN_EXPIRE=120;

    /**
     * 自动加载队列
     */
    private ConcurrentHashMap<String, AutoLoadTO> autoLoadMap;

    private ICacheManager cacheManager;

    /**
     * 缓存池
     */
    private Thread threads[];

    /**
     * 排序进行，对自动加载队列进行排序
     */
    private Thread sortThread;

    /**
     * 自动加载队列
     */
    private LinkedBlockingQueue<AutoLoadTO> autoLoadQueue;

    private volatile boolean running=false;

    /**
     * 自动加载配置
     */
    private AutoLoadConfig config;

    /**
     * @param cacheManager 缓存的set,get方法实现类
     * @param config 配置
     */
    public AutoLoadHandler(ICacheManager cacheManager, AutoLoadConfig config) {
        this.cacheManager=cacheManager;
        this.config=config;
        this.running=true;
        this.threads=new Thread[this.config.getThreadCnt()];
        this.autoLoadMap=new ConcurrentHashMap<String, AutoLoadTO>(this.config.getMaxElement());
        this.autoLoadQueue=new LinkedBlockingQueue<AutoLoadTO>(this.config.getMaxElement());
        this.sortThread=new Thread(new SortRunnable());
        this.sortThread.start();
        for(int i=0; i < this.config.getThreadCnt(); i++) {
            this.threads[i]=new Thread(new AutoLoadRunnable());
            this.threads[i].start();
        }
    }

    public AutoLoadConfig getConfig() {
        return config;
    }

    public int getSize() {
        if(null != autoLoadMap) {
            return autoLoadMap.size();
        }
        return -1;
    }

    public AutoLoadTO getAutoLoadTO(CacheKeyTO cacheKey) {
        if(null == autoLoadMap) {
            return null;
        }
        return autoLoadMap.get(cacheKey.getFullKey());
    }

    public void removeAutoLoadTO(CacheKeyTO cacheKey) {
        if(null == autoLoadMap) {
            return;
        }
        autoLoadMap.remove(cacheKey.getFullKey());
    }

    /**
     * 重置自动加载时间
     * @param cacheKey 缓存Key
     */
    public void resetAutoLoadLastLoadTime(CacheKeyTO cacheKey) {
        AutoLoadTO autoLoadTO=autoLoadMap.get(cacheKey.getFullKey());
        if(null != autoLoadTO && !autoLoadTO.isLoading()) {
            autoLoadTO.setLastLoadTime(1L);
        }
    }

    public void shutdown() {
        running=false;
        autoLoadMap.clear();
        autoLoadMap=null;
        logger.info("----------------------AutoLoadHandler.shutdown--------------------");
    }

    public AutoLoadTO putIfAbsent(CacheKeyTO cacheKey, CacheAopProxyChain joinPoint, Cache cache, ISerializer<Object> serializer,
        CacheWrapper cacheWrapper) {
        if(null == autoLoadMap) {
            return null;
        }
        int expire=cacheWrapper.getExpire();
        if(cacheWrapper.getExpire() >= AUTO_LOAD_MIN_EXPIRE && autoLoadMap.size() <= this.config.getMaxElement()) {
            Object[] arguments=joinPoint.getArgs();
            try {
                arguments=(Object[])BeanUtil.deepClone(arguments, serializer); // 进行深度复制
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                return null;
            }
            AutoLoadTO autoLoadTO=new AutoLoadTO(cacheKey, joinPoint, arguments, cache, expire);
            AutoLoadTO tmp=autoLoadMap.putIfAbsent(cacheKey.getFullKey(), autoLoadTO);
            if(null == tmp) {
                return autoLoadTO;
            } else {
                return tmp;
            }
        }
        return null;
    }

    /**
     * 获取自动加载队列，如果是web应用，建议把自动加载队列中的数据都输出到页面中，并增加一些管理功能。
     * @return autoload 队列
     */
    public AutoLoadTO[] getAutoLoadQueue() {
        if(autoLoadMap.isEmpty()) {
            return null;
        }
        AutoLoadTO tmpArr[]=new AutoLoadTO[autoLoadMap.size()];
        tmpArr=autoLoadMap.values().toArray(tmpArr);// 复制引用
        if(null != config.getSortType() && null != config.getSortType().getComparator()) {
            Arrays.sort(tmpArr, config.getSortType().getComparator());
        }
        return tmpArr;
    }

    class SortRunnable implements Runnable {

        @Override
        public void run() {
            while(running) {
                int sleep=100;
                if(autoLoadMap.isEmpty() || autoLoadQueue.size() > 0) {// 如果没有数据 或 还有线程在处理，则继续等待
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                    continue;
                } else if(autoLoadMap.size() <= threads.length * 10) {
                    sleep=1000;
                } else if(autoLoadMap.size() <= threads.length * 50) {
                    sleep=300;
                }
                try {
                    Thread.sleep(sleep);
                } catch(InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }

                AutoLoadTO tmpArr[]=getAutoLoadQueue();
                if(null == tmpArr) {
                    continue;
                }
                int i=0;
                for(AutoLoadTO to: tmpArr) {
                    try {
                        autoLoadQueue.put(to);
                        i++;
                        if(i == 2000) {
                            i=0;
                            Thread.sleep(0);// 触发操作系统立刻重新进行一次CPU竞争, 让其它线程获得CPU控制权的权力。
                        }
                    } catch(InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    } catch(Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    class AutoLoadRunnable implements Runnable {

        @Override
        public void run() {
            while(running) {
                try {
                    AutoLoadTO tmpTO=autoLoadQueue.take();
                    if(null != tmpTO) {
                        loadCache(tmpTO);
                        Thread.sleep(config.getAutoLoadPeriod());
                    }
                } catch(InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        private void loadCache(AutoLoadTO autoLoadTO) {
            if(null == autoLoadTO) {
                return;
            }
            long now=System.currentTimeMillis();
            if(autoLoadTO.getLastRequestTime() <= 0 || autoLoadTO.getLastLoadTime() <= 0) {
                return;
            }
            Cache cache=autoLoadTO.getCache();
            long requestTimeout=cache.requestTimeout();
            if(requestTimeout > 0 && (now - autoLoadTO.getLastRequestTime()) >= requestTimeout * 1000) {// 如果超过一定时间没有请求数据，则从队列中删除
                autoLoadMap.remove(autoLoadTO.getCacheKey().getFullKey());
                return;
            }
            if(autoLoadTO.getLoadCnt() > 100 && autoLoadTO.getAverageUseTime() < 10) {// 如果效率比较高的请求，就没必要使用自动加载了。
                autoLoadMap.remove(autoLoadTO.getCacheKey().getFullKey());
                return;
            }
            // 对于使用频率很低的数据，也可以考虑不用自动加载
            long difFirstRequestTime=now - autoLoadTO.getFirstRequestTime();
            long oneHourSecs=3600000L;
            if(difFirstRequestTime > oneHourSecs && autoLoadTO.getAverageUseTime() < 1000
                && (autoLoadTO.getRequestTimes() / (difFirstRequestTime / oneHourSecs)) < 60) {// 使用率比较低的数据，没有必要使用自动加载。
                autoLoadMap.remove(autoLoadTO.getCacheKey().getFullKey());
                return;
            }
            if(autoLoadTO.isLoading()) {
                return;
            }
            int expire=autoLoadTO.getExpire();
            if(expire == 0) {
                return;
            }
            // 计算超时时间
            int alarmTime=autoLoadTO.getCache().alarmTime();
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
            timeout*=1000;
            if((now - autoLoadTO.getLastLoadTime()) < timeout) {
                return;
            }
            if(config.isCheckFromCacheBeforeLoad()) {
                CacheWrapper result=cacheManager.get(autoLoadTO.getCacheKey());
                if(null != result) {// 如果已经被别的服务器更新了，则不需要再次更新
                    autoLoadTO.setExpire(result.getExpire());
                    if(result.getLastLoadTime() > autoLoadTO.getLastLoadTime() && (now - result.getLastLoadTime()) < timeout) {
                        autoLoadTO.setLastLoadTime(result.getLastLoadTime());
                        return;
                    }
                }
            }
            try {
                CacheAopProxyChain pjp=autoLoadTO.getJoinPoint();
                cacheManager.loadData(pjp, autoLoadTO, autoLoadTO.getCacheKey(), autoLoadTO.getCache());
            } catch(Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
