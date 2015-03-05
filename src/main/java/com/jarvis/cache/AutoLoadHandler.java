package com.jarvis.cache;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;

import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.type.AutoLoadQueueSortType;

/**
 * 用于处理自动加载缓存，sortThread 从autoLoadMap中取出数据，然后通知threads进行处理。
 * @author jiayu.qiu
 */
public class AutoLoadHandler<T> {

    private static final Logger logger=Logger.getLogger(AutoLoadHandler.class);

    /**
     * 自动加载队列
     */
    private Map<String, AutoLoadTO> autoLoadMap;

    /**
     * 自动加载队列中允许存放的最大容量
     */
    private int maxElement=10000;

    private CacheGeterSeter<T> cacheGeterSeter;

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

    /**
     * 自动加载队列排序算法
     */
    private AutoLoadQueueSortType sortType=AutoLoadQueueSortType.NONE;

    private boolean running=false;

    /**
     * @param threadCnt 线程数量
     * @param cacheGeterSeter 缓存的set,get方法实现类
     * @param maxElement 自动加载队列的容量
     * @param sortQueue 是否对自动加载队列进行排序
     */
    public AutoLoadHandler(int threadCnt, CacheGeterSeter<T> cacheGeterSeter, int maxElement, AutoLoadQueueSortType sortType) {
        if(threadCnt <= 0) {
            return;
        }
        this.cacheGeterSeter=cacheGeterSeter;
        this.maxElement=maxElement;
        this.sortType=sortType;
        this.running=true;
        this.threads=new Thread[threadCnt];
        this.autoLoadMap=new ConcurrentHashMap<String, AutoLoadTO>(this.maxElement);
        this.autoLoadQueue=new LinkedBlockingQueue<AutoLoadTO>(this.maxElement);
        for(int i=0; i < threadCnt; i++) {
            this.threads[i]=new Thread(new AutoLoadRunnable());
            this.threads[i].start();
        }
        this.sortThread=new Thread(new SortRunnable());
        this.sortThread.start();
    }

    public AutoLoadHandler(int threadCnt, CacheGeterSeter<T> cacheGeterSeter, int maxElement) {
        this(threadCnt, cacheGeterSeter, maxElement, AutoLoadQueueSortType.NONE);
    }

    public int getSize() {
        if(null != autoLoadMap) {
            return autoLoadMap.size();
        }
        return -1;
    }

    public AutoLoadTO getAutoLoadTO(String cacheKey) {
        if(null == autoLoadMap) {
            return null;
        }
        return autoLoadMap.get(cacheKey);
    }

    public void shutdown() {
        running=false;
        autoLoadMap.clear();
        autoLoadMap=null;
        logger.info("----------------------AutoLoadHandler.shutdown--------------------");
    }

    public void setAutoLoadTO(AutoLoadTO autoLoadTO) {
        if(null == autoLoadMap) {
            return;
        }
        if(autoLoadTO.getExpire() >= 120 && autoLoadMap.size() <= maxElement) {
            autoLoadMap.put(autoLoadTO.getCacheKey(), autoLoadTO);
        }
    }

    class SortRunnable implements Runnable {

        private final AutoLoadTOComparator comparator=new AutoLoadTOComparator();

        @Override
        public void run() {
            while(running) {
                if(autoLoadMap.isEmpty() || autoLoadQueue.size() > 0) {// 如果没有数据 或 还有线程在处理，则继续等待
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                    continue;
                }

                AutoLoadTO tmpArr[]=new AutoLoadTO[autoLoadMap.size()];
                tmpArr=autoLoadMap.values().toArray(tmpArr);// 复制引用
                if(null != sortType) {
                    switch(sortType) {
                        case OLDEST_FIRST:
                            Arrays.sort(tmpArr, comparator);
                            break;
                        default:
                            break;
                    }
                }
                for(AutoLoadTO to: tmpArr) {
                    try {
                        autoLoadQueue.put(to);
                    } catch(InterruptedException e) {
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
                        Thread.sleep(50);
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
            if(autoLoadTO.getLastRequestTime() <= 0 || autoLoadTO.getLastLoadTime() <= 0) {
                return;
            }
            if(autoLoadTO.getRequestTimeout() > 0
                && (System.currentTimeMillis() - autoLoadTO.getLastRequestTime()) >= autoLoadTO.getRequestTimeout() * 1000) {// 如果超过一定时间没有请求数据，则从队列中删除
                autoLoadMap.remove(autoLoadTO.getCacheKey());
                return;
            }
            if(autoLoadTO.getLoadCnt() > 100 && autoLoadTO.getAverageUseTime() < 200) {// 如果效率比较高的请求，就没必要使用自动加载了。
                autoLoadMap.remove(autoLoadTO.getCacheKey());
                return;
            }
            int diff;
            if(autoLoadTO.getExpire() >= 600) {
                diff=120;
            } else {
                diff=60;
            }
            if(!autoLoadTO.isLoading()
                && (System.currentTimeMillis() - autoLoadTO.getLastLoadTime()) >= (autoLoadTO.getExpire() - diff) * 1000) {
                try {
                    autoLoadTO.setLoading(true);
                    ProceedingJoinPoint pjp=autoLoadTO.getJoinPoint();
                    String className=pjp.getTarget().getClass().getName();
                    String methodName=pjp.getSignature().getName();
                    long startTime=System.currentTimeMillis();
                    @SuppressWarnings("unchecked")
                    T result=(T)pjp.proceed(autoLoadTO.getArgs());
                    long useTime=System.currentTimeMillis() - startTime;
                    if(useTime >= 500) {
                        logger.error(className + "." + methodName + ", use time:" + useTime + "ms");
                    }
                    CacheWrapper<T> tmp=new CacheWrapper<T>();
                    tmp.setCacheObject(result);
                    tmp.setLastLoadTime(System.currentTimeMillis());
                    cacheGeterSeter.setCache(autoLoadTO.getCacheKey(), tmp, autoLoadTO.getExpire());
                    autoLoadTO.setLastLoadTime(System.currentTimeMillis());
                    autoLoadTO.addUseTotalTime(useTime);
                } catch(Exception e) {
                    logger.error(e.getMessage(), e);
                } catch(Throwable e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    autoLoadTO.setLoading(false);
                }
            }
        }
    }

}
