package com.jarvis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;

import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 用于处理自动加载缓存
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
    private long maxElement=10000;

    private CacheGeterSeter<T> cacheGeterSeter;
    
    /**
     * 缓存池
     */
    private Thread threads[];

    private boolean running=false;

    /**
     * 
     * @param threadCnt 线程数量
     * @param cacheGeterSeter 缓存的set,get方法实现类
     * @param maxElement 自动加载队列的容量
     */
    public AutoLoadHandler(int threadCnt, CacheGeterSeter<T> cacheGeterSeter, long maxElement) {
        if(threadCnt <= 0) {
            return;
        }
        this.cacheGeterSeter=cacheGeterSeter;
        this.maxElement=maxElement;
        running=true;
        threads=new Thread[threadCnt];
        autoLoadMap=new ConcurrentHashMap<String, AutoLoadTO>();
        for(int i=0; i < threadCnt; i++) {
            threads[i]=new Thread(new AutoLoadRunnable(i));
            threads[i].start();
        }
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

    class AutoLoadRunnable implements Runnable {

        int threadInd;

        public AutoLoadRunnable(int threadInd) {
            this.threadInd=threadInd;
        }

        @Override
        public void run() {
            while(running) {
                int ind=threadInd;
                if(autoLoadMap.isEmpty() || autoLoadMap.size() < (ind + 1)) {
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                long beginTime=System.currentTimeMillis();
                AutoLoadTO tos[]=new AutoLoadTO[autoLoadMap.size()];
                tos=autoLoadMap.values().toArray(tos);// 复制引用
                while(ind < tos.length) {
                    AutoLoadTO tmpTO=tos[ind];
                    ind+=threads.length;
                    loadCache(tmpTO);
                }
                long totalTime=System.currentTimeMillis() - beginTime;
                int sleep=1000;
                if(totalTime >= 1000) {
                    sleep=100;
                }
                try {
                    Thread.sleep(sleep);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void loadCache(AutoLoadTO autoLoadTO) {
            if(autoLoadTO.getLastRequestTime() <= 0 || autoLoadTO.getLastLoadTime() <= 0) {
                return;
            }
            if(autoLoadTO.getRequestTimeout() > 0 && (System.currentTimeMillis() - autoLoadTO.getLastRequestTime()) >= autoLoadTO.getRequestTimeout() * 1000) {// 如果超过一定时间没有请求数据，则从队列中删除
                autoLoadMap.remove(autoLoadTO.getCacheKey());
                return;
            }
            int diff;
            if(autoLoadTO.getExpire() >= 600) {
                diff=120;
            } else {
                diff=60;
            }
            if(!autoLoadTO.isLoading() && (System.currentTimeMillis() - autoLoadTO.getLastLoadTime()) >= (autoLoadTO.getExpire() - diff) * 1000) {
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
                } catch(Exception e) {
                    logger.error(e.getMessage(), e);
                } catch(Throwable e) {
                    logger.error(e.getMessage(), e);
                }
                autoLoadTO.setLoading(false);
            }
        }
    }

}
