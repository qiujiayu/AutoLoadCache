package com.jarvis.cache;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 用于处理自动加载缓存，sortThread 从autoLoadMap中取出数据，然后通知threads进行处理。
 * @author jiayu.qiu
 */
public class AutoLoadHandler {

    private static final Logger logger=LoggerFactory.getLogger(AutoLoadHandler.class);

    public static final Integer AUTO_LOAD_MIN_EXPIRE=120;

    public static final String THREAD_NAME_PREFIX="autoLoadThread-";

    /**
     * 自动加载队列
     */
    private final ConcurrentHashMap<CacheKeyTO, AutoLoadTO> autoLoadMap;

    private final CacheHandler cacheHandler;

    /**
     * 缓存池
     */
    private final Thread threads[];

    /**
     * 排序进行，对自动加载队列进行排序
     */
    private final Thread sortThread;

    /**
     * 自动加载队列
     */
    private final LinkedBlockingQueue<AutoLoadTO> autoLoadQueue;

    private volatile boolean running=false;

    /**
     * 自动加载配置
     */
    private final AutoLoadConfig config;

    /**
     * 随机数种子
     */
    private static final ThreadLocal<Random> random=new ThreadLocal<Random>() {

        @Override
        protected Random initialValue() {
            return new Random();
        }

    };

    /**
     * @param cacheHandler 缓存的set,get方法实现类
     * @param config 配置
     */
    public AutoLoadHandler(CacheHandler cacheHandler, AutoLoadConfig config) {
        this.cacheHandler=cacheHandler;
        this.config=config;
        if(this.config.getThreadCnt() > 0) {
            this.running=true;
            this.threads=new Thread[this.config.getThreadCnt()];
            this.autoLoadMap=new ConcurrentHashMap<CacheKeyTO, AutoLoadTO>(this.config.getMaxElement());
            this.autoLoadQueue=new LinkedBlockingQueue<AutoLoadTO>(this.config.getMaxElement());
            this.sortThread=new Thread(new SortRunnable());
            this.sortThread.setDaemon(true);
            this.sortThread.start();
            for(int i=0; i < this.config.getThreadCnt(); i++) {
                this.threads[i]=new Thread(new AutoLoadRunnable());
                this.threads[i].setName(THREAD_NAME_PREFIX + i);
                this.threads[i].setDaemon(true);
                this.threads[i].start();
            }
        } else {
            this.threads=null;
            this.autoLoadMap=null;
            this.autoLoadQueue=null;
            this.sortThread=null;
        }
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
        return autoLoadMap.get(cacheKey);
    }

    public void removeAutoLoadTO(CacheKeyTO cacheKey) {
        if(null == autoLoadMap) {
            return;
        }
        autoLoadMap.remove(cacheKey);
    }

    /**
     * 重置自动加载时间
     * @param cacheKey 缓存Key
     */
    public void resetAutoLoadLastLoadTime(CacheKeyTO cacheKey) {
        if(null == autoLoadMap) {
            return;
        }
        AutoLoadTO autoLoadTO=autoLoadMap.get(cacheKey);
        if(null != autoLoadTO && !autoLoadTO.isLoading()) {
            autoLoadTO.setLastLoadTime(1L);
        }
    }

    public void shutdown() {
        running=false;
        if(null != autoLoadMap) {
            autoLoadMap.clear();
        }
        logger.info("----------------------AutoLoadHandler.shutdown--------------------");
    }

    public AutoLoadTO putIfAbsent(CacheKeyTO cacheKey, CacheAopProxyChain joinPoint, Cache cache, CacheWrapper<Object> cacheWrapper) {
        if(null == autoLoadMap) {
            return null;
        }
        AutoLoadTO autoLoadTO=autoLoadMap.get(cacheKey);
        if(null != autoLoadTO) {
            return autoLoadTO;
        }
        try {
            if(!cacheHandler.getScriptParser().isAutoload(cache, joinPoint.getArgs(), cacheWrapper.getCacheObject())) {
                return null;
            }
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        int expire=cacheWrapper.getExpire();
        if(expire >= AUTO_LOAD_MIN_EXPIRE && autoLoadMap.size() <= this.config.getMaxElement()) {
            Object[] arguments=joinPoint.getArgs();
            try {
                arguments=(Object[])cacheHandler.getCloner().deepCloneMethodArgs(joinPoint.getMethod(), arguments); // 进行深度复制
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                return null;
            }
            autoLoadTO=new AutoLoadTO(cacheKey, joinPoint, arguments, cache, expire);
            AutoLoadTO tmp=autoLoadMap.putIfAbsent(cacheKey, autoLoadTO);
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
        if(null == autoLoadMap || autoLoadMap.isEmpty()) {
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
                if(null == tmpArr || tmpArr.length == 0) {
                    continue;
                }
                for(int i=0; i < tmpArr.length; i++) {
                    try {
                        AutoLoadTO to=tmpArr[i];
                        autoLoadQueue.put(to);
                        if(i > 0 && i % 2000 == 0) {
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
                autoLoadMap.remove(autoLoadTO.getCacheKey());
                return;
            }
            if(autoLoadTO.getLoadCnt() > 100 && autoLoadTO.getAverageUseTime() < 10) {// 如果效率比较高的请求，就没必要使用自动加载了。
                autoLoadMap.remove(autoLoadTO.getCacheKey());
                return;
            }
            // 对于使用频率很低的数据，也可以考虑不用自动加载
            long difFirstRequestTime=now - autoLoadTO.getFirstRequestTime();
            long oneHourSecs=3600000L;
            if(difFirstRequestTime > oneHourSecs && autoLoadTO.getAverageUseTime() < 1000 && (autoLoadTO.getRequestTimes() / (difFirstRequestTime / oneHourSecs)) < 60) {// 使用率比较低的数据，没有必要使用自动加载。
                autoLoadMap.remove(autoLoadTO.getCacheKey());
                return;
            }
            if(autoLoadTO.isLoading()) {
                return;
            }
            int expire=autoLoadTO.getExpire();
            if(expire < AUTO_LOAD_MIN_EXPIRE) {// 如果过期时间太小了，就不允许自动加载，避免加载过于频繁，影响系统稳定性
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
            int rand=random.get().nextInt(10);
            timeout=(timeout + (rand % 2 == 0 ? rand : -rand)) * 1000;
            if((now - autoLoadTO.getLastLoadTime()) < timeout) {
                return;
            }
            CacheWrapper<Object> result=null;
            if(config.isCheckFromCacheBeforeLoad()) {
                try {
                    Method method=autoLoadTO.getJoinPoint().getMethod();
                    // Type returnType=method.getGenericReturnType();
                    result=cacheHandler.get(autoLoadTO.getCacheKey(), method, autoLoadTO.getArgs());
                } catch(Exception ex) {

                }
                if(null != result) {// 如果已经被别的服务器更新了，则不需要再次更新
                    autoLoadTO.setExpire(result.getExpire());
                    if(result.getLastLoadTime() > autoLoadTO.getLastLoadTime() && (now - result.getLastLoadTime()) < timeout) {
                        autoLoadTO.setLastLoadTime(result.getLastLoadTime());
                        return;
                    }
                }
            }
            CacheAopProxyChain pjp=autoLoadTO.getJoinPoint();
            CacheKeyTO cacheKey=autoLoadTO.getCacheKey();
            DataLoaderFactory factory=DataLoaderFactory.getInstance();
            DataLoader dataLoader=factory.getDataLoader();
            CacheWrapper<Object> newCacheWrapper=null;
            try {
                newCacheWrapper=dataLoader.init(pjp, autoLoadTO, cacheKey, cache, cacheHandler).loadData().getCacheWrapper();
            } catch(Throwable e) {
                logger.error(e.getMessage(), e);
            }
            long loadDataUseTime=dataLoader.getLoadDataUseTime();
            boolean isFirst=dataLoader.isFirst();
            factory.returnObject(dataLoader);
            if(isFirst) {
                if(null == newCacheWrapper && null != result) {// 如果加载失败，则把旧数据进行续租
                    int newExpire=AUTO_LOAD_MIN_EXPIRE + 60;
                    newCacheWrapper=new CacheWrapper<Object>(result.getCacheObject(), newExpire);
                }
                try {
                    if(null != newCacheWrapper) {
                        cacheHandler.writeCache(pjp, autoLoadTO.getArgs(), cache, cacheKey, newCacheWrapper);
                        autoLoadTO.setLastLoadTime(newCacheWrapper.getLastLoadTime())// 同步加载时间
                            .setExpire(newCacheWrapper.getExpire())// 同步过期时间
                            .addUseTotalTime(loadDataUseTime);
                    }
                } catch(Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
