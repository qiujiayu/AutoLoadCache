package com.jarvis.cache.to;

import java.util.Map;

import com.jarvis.cache.type.AutoLoadQueueSortType;
import lombok.ToString;

/**
 * 缓存处理的相关 配置
 * 
 * @author jiayu.qiu
 */
@ToString
public class AutoLoadConfig {

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 处理自动加载队列的线程数量
     */
    private Integer threadCnt = 10;

    /**
     * 自动加载队列中允许存放的最大容量
     */
    private int maxElement = 20000;

    /**
     * 是否打印比较耗时的请求
     */
    private boolean printSlowLog = true;

    /**
     * 当请求耗时超过此值时，记录目录（printSlowLog=true 时才有效），单位：毫秒
     */
    private int slowLoadTime = 500;

    /**
     * 自动加载队列排序算法
     */
    private AutoLoadQueueSortType sortType = AutoLoadQueueSortType.NONE;

    /**
     * 加载数据之前去缓存服务器中检查，数据是否快过期，如果应用程序部署的服务器数量比较少，设置为false,
     * 如果部署的服务器比较多，可以考虑设置为true
     */
    private boolean checkFromCacheBeforeLoad = false;

    /**
     * 单个线程中执行自动加载的时间间隔
     */
    private int autoLoadPeriod = 50;

    /**
     * 异步刷新缓存线程池的 corePoolSize
     */
    private int refreshThreadPoolSize = 2;

    /**
     * 异步刷新缓存线程池的 maximumPoolSize
     */
    private int refreshThreadPoolMaxSize = 20;

    /**
     * 异步刷新缓存线程池的 keepAliveTime
     */
    private int refreshThreadPoolkeepAliveTime = 20;// 单位：分钟

    /**
     * 异步刷新缓存队列容量
     */
    private int refreshQueueCapacity = 2000;

    private Map<String, String> functions;

    /**
     * 加载数据重试次数，默认值为1：
     */
    private int loadDataTryCnt = 1;

    /**
     * Processing Map的初始大小
     */
    private int processingMapSize = 512;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getThreadCnt() {
        return threadCnt;
    }

    public void setThreadCnt(int threadCnt) {
        if (threadCnt <= 0) {
            return;
        }
        this.threadCnt = threadCnt;
    }

    public int getMaxElement() {
        return maxElement;
    }

    public void setMaxElement(int maxElement) {
        if (maxElement <= 0) {
            return;
        }
        this.maxElement = maxElement;
    }

    public AutoLoadQueueSortType getSortType() {
        return sortType;
    }

    public void setSortType(Integer sortType) {
        this.sortType = AutoLoadQueueSortType.getById(sortType);
    }

    public boolean isPrintSlowLog() {
        return printSlowLog;
    }

    public void setPrintSlowLog(boolean printSlowLog) {
        this.printSlowLog = printSlowLog;
    }

    public int getSlowLoadTime() {
        return slowLoadTime;
    }

    public void setSlowLoadTime(int slowLoadTime) {
        if (slowLoadTime < 0) {
            return;
        }
        this.slowLoadTime = slowLoadTime;
    }

    public boolean isCheckFromCacheBeforeLoad() {
        return checkFromCacheBeforeLoad;
    }

    public void setCheckFromCacheBeforeLoad(boolean checkFromCacheBeforeLoad) {
        this.checkFromCacheBeforeLoad = checkFromCacheBeforeLoad;
    }

    public int getAutoLoadPeriod() {
        return autoLoadPeriod;
    }

    public void setAutoLoadPeriod(int autoLoadPeriod) {
        int defaultPeriod = 5;
        if (autoLoadPeriod < defaultPeriod) {
            return;
        }
        this.autoLoadPeriod = autoLoadPeriod;
    }

    /**
     * 为表达式注册自定义函数
     * 
     * @param funcs 函数
     */
    public void setFunctions(Map<String, String> funcs) {
        if (null == funcs || funcs.isEmpty()) {
            return;
        }
        functions = funcs;
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public int getRefreshThreadPoolSize() {
        return refreshThreadPoolSize;
    }

    public void setRefreshThreadPoolSize(int refreshThreadPoolSize) {
        if (refreshThreadPoolSize > 1) {
            this.refreshThreadPoolSize = refreshThreadPoolSize;
        }
    }

    public int getRefreshThreadPoolMaxSize() {
        return refreshThreadPoolMaxSize;
    }

    public void setRefreshThreadPoolMaxSize(int refreshThreadPoolMaxSize) {
        if (refreshThreadPoolMaxSize > 1) {
            this.refreshThreadPoolMaxSize = refreshThreadPoolMaxSize;
        }
    }

    public int getRefreshThreadPoolkeepAliveTime() {
        return refreshThreadPoolkeepAliveTime;
    }

    public void setRefreshThreadPoolkeepAliveTime(int refreshThreadPoolkeepAliveTime) {
        if (refreshThreadPoolkeepAliveTime > 1) {
            this.refreshThreadPoolkeepAliveTime = refreshThreadPoolkeepAliveTime;
        }
    }

    public int getRefreshQueueCapacity() {
        return refreshQueueCapacity;
    }

    public void setRefreshQueueCapacity(int refreshQueueCapacity) {
        if (refreshQueueCapacity > 1) {
            this.refreshQueueCapacity = refreshQueueCapacity;
        }
    }

    public int getLoadDataTryCnt() {
        return loadDataTryCnt;
    }

    public void setLoadDataTryCnt(int loadDataTryCnt) {
        int minCnt = 0;
        int maxCnt = 5;
        if (loadDataTryCnt >= minCnt && loadDataTryCnt < maxCnt) {
            this.loadDataTryCnt = loadDataTryCnt;
        }
    }

    public int getProcessingMapSize() {
        return processingMapSize;
    }

    public void setProcessingMapSize(int processingMapSize) {
        int minSize = 64;
        if (processingMapSize < minSize) {
            this.processingMapSize = minSize;
        } else {
            this.processingMapSize = processingMapSize;
        }
    }

}
