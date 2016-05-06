package com.jarvis.cache.map;

import java.util.concurrent.ConcurrentHashMap;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

public class CachePointCut extends AbstractCacheManager {

    private final ConcurrentHashMap<String, Object> cache=new ConcurrentHashMap<String, Object>();

    /**
     * 缓存是否被修改过
     */
    private volatile boolean cacheChaned=false;

    private Thread thread=null;

    private CacheTask cacheTask=null;

    /**
     * 缓存持久化文件
     */
    private String persistFile;

    public CachePointCut(AutoLoadConfig config) {
        super(config);
    }

    public synchronized void start() {
        if(null == thread) {
            cacheTask=new CacheTask(this);
            thread=new Thread(cacheTask);
            cacheTask.start();
            thread.start();
        }
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        cacheTask.destroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setCache(CacheKeyTO cacheKeyTO, CacheWrapper result) {
        if(null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        String hfield=cacheKeyTO.getHfield();
        if(null == hfield || hfield.length() == 0) {
            cache.put(cacheKey, result);
        } else {
            ConcurrentHashMap<String, CacheWrapper> hash=(ConcurrentHashMap<String, CacheWrapper>)cache.get(cacheKey);
            if(null == hash) {
                hash=new ConcurrentHashMap<String, CacheWrapper>();
                ConcurrentHashMap<String, CacheWrapper> _hash=null;
                _hash=(ConcurrentHashMap<String, CacheWrapper>)cache.putIfAbsent(cacheKey, hash);
                if(null != _hash) {
                    hash=_hash;
                }
            }
            hash.put(hfield, result);
        }
        this.cacheChaned=true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper get(CacheKeyTO cacheKeyTO) {
        if(null == cacheKeyTO) {
            return null;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return null;
        }
        Object obj=cache.get(cacheKey);
        if(null == obj) {
            return null;
        }
        String hfield=cacheKeyTO.getHfield();
        if(null == hfield || hfield.length() == 0) {
            return (CacheWrapper)obj;
        } else {
            ConcurrentHashMap<String, CacheWrapper> hash=(ConcurrentHashMap<String, CacheWrapper>)obj;
            return hash.get(hfield);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void delete(CacheKeyTO cacheKeyTO) {
        if(null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        String hfield=cacheKeyTO.getHfield();
        if(null == hfield || hfield.length() == 0) {
            Object tmp=cache.remove(cacheKey);
            if(null != tmp) {// 如果删除成功
                this.cacheChaned=true;
            }
        } else {
            ConcurrentHashMap<String, CacheWrapper> hash=(ConcurrentHashMap<String, CacheWrapper>)cache.get(cacheKey);
            if(null != hash) {
                Object tmp=hash.remove(hfield);
                if(null != tmp) {// 如果删除成功
                    this.cacheChaned=true;
                }
            }
        }

    }

    public boolean isCacheChaned() {
        return cacheChaned;
    }

    public void setCacheChaned(boolean cacheChaned) {
        this.cacheChaned=cacheChaned;
    }

    public ConcurrentHashMap<String, Object> getCache() {
        return cache;
    }

    public String getPersistFile() {
        return persistFile;
    }

    public void setPersistFile(String persistFile) {
        this.persistFile=persistFile;
    }

}
