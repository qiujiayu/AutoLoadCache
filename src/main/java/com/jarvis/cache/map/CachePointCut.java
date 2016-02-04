package com.jarvis.cache.map;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

public class CachePointCut extends AbstractCacheManager<Object> implements Runnable {

    private static final Logger logger=Logger.getLogger(CachePointCut.class);

    private final ConcurrentHashMap<String, Object> cache=new ConcurrentHashMap<String, Object>();

    private int period=2 * 60 * 1000; // 2Minutes

    private boolean running=false;

    private Thread thread=null;

    public void start() {
        if(null == thread) {
            thread=new Thread(this);
        }
        if(!this.running) {
            this.running=true;
            thread.start();
        }
    }

    public void shutDown() {
        this.running=false;
    }

    public void run() {
        while(running) {
            try {
                cleanCache();
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(period);
            } catch(InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 清除过期缓存
     */
    @SuppressWarnings("unchecked")
    private void cleanCache() {
        Iterator<Entry<String, Object>> iterator=cache.entrySet().iterator();
        while(iterator.hasNext()) {
            Object value=iterator.next().getValue();
            if(value instanceof CacheWrapper) {
                CacheWrapper<Object> tmp=(CacheWrapper<Object>)value;
                if(tmp.isExpired()) {
                    iterator.remove();
                }
            } else {
                ConcurrentHashMap<String, CacheWrapper<Object>> hash=(ConcurrentHashMap<String, CacheWrapper<Object>>)value;
                Iterator<Entry<String, CacheWrapper<Object>>> iterator2=hash.entrySet().iterator();
                while(iterator2.hasNext()) {
                    CacheWrapper<Object> tmp=iterator2.next().getValue();
                    if(tmp.isExpired()) {
                        iterator2.remove();
                    }
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.running=false;
    }

    public CachePointCut(AutoLoadConfig config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setCache(CacheKeyTO cacheKeyTO, CacheWrapper<Object> result) {
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
            ConcurrentHashMap<String, CacheWrapper<Object>> hash=
                (ConcurrentHashMap<String, CacheWrapper<Object>>)cache.get(cacheKey);
            if(null == hash) {
                hash=new ConcurrentHashMap<String, CacheWrapper<Object>>();
                ConcurrentHashMap<String, CacheWrapper<Object>> _hash=
                    (ConcurrentHashMap<String, CacheWrapper<Object>>)cache.putIfAbsent(cacheKey, hash);
                if(null != _hash) {
                    hash=_hash;
                }
            }
            hash.put(hfield, result);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(CacheKeyTO cacheKeyTO) {
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
            ConcurrentHashMap<String, CacheWrapper<Object>> hash=(ConcurrentHashMap<String, CacheWrapper<Object>>)obj;
            return hash.get(hfield);
        } else {
            return (CacheWrapper<Object>)obj;
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
            cache.remove(cacheKey);
        } else {
            ConcurrentHashMap<String, CacheWrapper<Object>> hash=
                (ConcurrentHashMap<String, CacheWrapper<Object>>)cache.get(cacheKey);
            if(null == hash) {
                return;
            }
            hash.remove(hfield);
        }

    }

}
