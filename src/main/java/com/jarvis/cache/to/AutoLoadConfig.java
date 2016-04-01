package com.jarvis.cache.to;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import com.jarvis.cache.CacheUtil;
import com.jarvis.cache.type.AutoLoadQueueSortType;

/**
 * AutoLoadHandler 配置
 * @author jiayu.qiu
 */
public class AutoLoadConfig {

    /**
     * 处理自动加载队列的线程数量
     */
    private Integer threadCnt=10;

    /**
     * 自动加载队列中允许存放的最大容量
     */
    private int maxElement=20000;

    /**
     * 是否打印比较耗时的请求
     */
    private boolean printSlowLog=true;

    /**
     * 当请求耗时超过此值时，记录目录（printSlowLog=true 时才有效），单位：毫秒
     */
    private int slowLoadTime=500;

    /**
     * 自动加载队列排序算法
     */
    private AutoLoadQueueSortType sortType=AutoLoadQueueSortType.NONE;

    /**
     * 加载数据之前去缓存服务器中检查，数据是否快过期，如果应用程序部署的服务器数量比较少，设置为false, 如果部署的服务器比较多，可以考虑设置为true
     */
    private boolean checkFromCacheBeforeLoad=false;

    /**
     * 单个线程中执行自动加载的时间间隔
     */
    private int autoLoadPeriod=50;

    public int getThreadCnt() {
        return threadCnt;
    }

    public void setThreadCnt(int threadCnt) {
        if(threadCnt <= 0) {
            return;
        }
        this.threadCnt=threadCnt;
    }

    public int getMaxElement() {
        return maxElement;
    }

    public void setMaxElement(int maxElement) {
        if(maxElement <= 0) {
            return;
        }
        this.maxElement=maxElement;
    }

    public AutoLoadQueueSortType getSortType() {
        return sortType;
    }

    public void setSortType(Integer sortType) {
        this.sortType=AutoLoadQueueSortType.getById(sortType);
    }

    public boolean isPrintSlowLog() {
        return printSlowLog;
    }

    public void setPrintSlowLog(boolean printSlowLog) {
        this.printSlowLog=printSlowLog;
    }

    public int getSlowLoadTime() {
        return slowLoadTime;
    }

    public void setSlowLoadTime(int slowLoadTime) {
        if(slowLoadTime < 0) {
            return;
        }
        this.slowLoadTime=slowLoadTime;
    }

    public boolean isCheckFromCacheBeforeLoad() {
        return checkFromCacheBeforeLoad;
    }

    public void setCheckFromCacheBeforeLoad(boolean checkFromCacheBeforeLoad) {
        this.checkFromCacheBeforeLoad=checkFromCacheBeforeLoad;
    }

    public int getAutoLoadPeriod() {
        return autoLoadPeriod;
    }

    public void setAutoLoadPeriod(int autoLoadPeriod) {
        if(autoLoadPeriod < 5) {
            return;
        }
        this.autoLoadPeriod=autoLoadPeriod;
    }

    /**
     * 为Spring EL注册自定义函数
     * @param funcs
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setFunctions(Map<String, String> funcs) {
        if(null == funcs || funcs.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, String>> it=funcs.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, String> entry=it.next();
            try {
                String name=entry.getKey();
                Class cls=Class.forName(entry.getValue());
                Method method=cls.getDeclaredMethod(name, new Class[]{Object.class});
                CacheUtil.addFunction(name, method);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}
