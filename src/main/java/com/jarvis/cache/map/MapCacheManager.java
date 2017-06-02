package com.jarvis.cache.map;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 使用ConcurrentHashMap管理缓存
 * @author jiayu.qiu
 */
public class MapCacheManager implements ICacheManager {

    private static final Logger logger=LoggerFactory.getLogger(MapCacheManager.class);

    private final ConcurrentHashMap<String, Object> cache=new ConcurrentHashMap<String, Object>();

    private final CacheChangeListener changeListener;

    private final ISerializer<Object> serializer;

    private final ICloner cloner;

    private final AutoLoadConfig config;

    /**
     * 允许不持久化变更数(当缓存变更数量超过此值才做持久化操作)
     */
    private int unpersistMaxSize=0;

    private Thread thread=null;

    private CacheTask cacheTask=null;

    /**
     * 缓存持久化文件
     */
    private String persistFile;

    /**
     * 是否在持久化:为true时，允许持久化，false，不允许持久化
     */
    private boolean needPersist=true;

    /**
     * 从缓存中取数据时，是否克隆：true时，是克隆缓存值，可以避免外界修改缓存值；false，不克隆缓存值，缓存中的数据可能被外界修改，但效率比较高。
     */
    private boolean copyValueOnGet=false;

    /**
     * 往缓存中写数据时，是否把克隆后的值放入缓存：true时，是拷贝缓存值，可以避免外界修改缓存值；false，不拷贝缓存值，缓存中的数据可能被外界修改，但效率比较高。
     */
    private boolean copyValueOnSet=false;

    /**
     * 清除和持久化的时间间隔
     */
    private int clearAndPersistPeriod=60 * 1000; // 1Minutes

    public MapCacheManager(AutoLoadConfig config, ISerializer<Object> serializer) {
        this.config=config;
        this.config.setCheckFromCacheBeforeLoad(false);
        this.serializer=serializer;
        this.cloner=serializer;
        cacheTask=new CacheTask(this);
        changeListener=cacheTask;
    }

    public synchronized void start() {
        if(null == thread) {
            thread=new Thread(cacheTask);
            cacheTask.start();
            thread.start();
        }
    }

    public synchronized void destroy() {
        cacheTask.destroy();
        if(thread != null) {
            thread.interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method, final Object args[]) throws CacheCenterConnectionException {
        if(null == cacheKeyTO) {
            return;
        }
        if(result.getExpire() < 0) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        CacheWrapper<Object> value=null;
        if(copyValueOnSet) {
            try {
                value=(CacheWrapper<Object>)this.getCloner().deepClone(result, null);// 这里type为null，因为有可能是设置@ExCache缓存
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            value=result;
        }
        SoftReference<CacheWrapper<Object>> reference=new SoftReference<CacheWrapper<Object>>(value);
        String hfield=cacheKeyTO.getHfield();
        if(null == hfield || hfield.length() == 0) {
            cache.put(cacheKey, reference);
        } else {
            Object tmpObj=cache.get(cacheKey);
            ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>> hash;
            if(null == tmpObj) {
                hash=new ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>>();
                ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>> _hash=null;
                _hash=(ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>>)cache.putIfAbsent(cacheKey, hash);
                if(null != _hash) {
                    hash=_hash;
                }
            } else {
                if(tmpObj instanceof ConcurrentHashMap) {
                    hash=(ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>>)tmpObj;
                } else {
                    logger.error(method.getClass().getName() + "." + method.getName() + "中key为" + cacheKey + "的缓存，已经被占用，请删除缓存再试。");
                    return;
                }
            }
            hash.put(hfield, reference);
        }
        this.changeListener.cacheChange();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(final CacheKeyTO cacheKeyTO, final Method method, final Object args[]) throws CacheCenterConnectionException {
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
        CacheWrapper<Object> value=null;
        if(null == hfield || hfield.length() == 0) {
            if(obj instanceof SoftReference) {
                SoftReference<CacheWrapper<Object>> reference=(SoftReference<CacheWrapper<Object>>)obj;
                if(null != reference) {
                    value=reference.get();
                }
            } else if(obj instanceof CacheWrapper) {// 兼容老版本
                value=(CacheWrapper<Object>)obj;
            }
        } else {
            ConcurrentHashMap<String, Object> hash=(ConcurrentHashMap<String, Object>)obj;
            Object tmp=hash.get(hfield);
            if(tmp instanceof SoftReference) {
                SoftReference<CacheWrapper<Object>> reference=(SoftReference<CacheWrapper<Object>>)tmp;
                if(null != reference) {
                    value=reference.get();
                }
            } else if(tmp instanceof CacheWrapper) {// 兼容老版本
                value=(CacheWrapper<Object>)tmp;
            }
        }
        if(null != value) {
            if(value.isExpired()) {
                return null;
            }
            if(copyValueOnGet) {
                try {
                    CacheWrapper<Object> res=(CacheWrapper<Object>)value.clone();
                    res.setCacheObject(this.getCloner().deepClone(value.getCacheObject(), method.getReturnType()));
                    return res;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void delete(CacheKeyTO cacheKeyTO) throws CacheCenterConnectionException {
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
            if(null == tmp) {// 如果删除失败
                return;
            }
            if(tmp instanceof CacheWrapper) {
                this.changeListener.cacheChange();
            } else if(tmp instanceof ConcurrentHashMap) {
                ConcurrentHashMap<String, CacheWrapper<Object>> hash=(ConcurrentHashMap<String, CacheWrapper<Object>>)tmp;
                if(hash.size() > 0) {
                    this.changeListener.cacheChange(hash.size());
                }
            }
        } else {
            ConcurrentHashMap<String, CacheWrapper<Object>> hash=(ConcurrentHashMap<String, CacheWrapper<Object>>)cache.get(cacheKey);
            if(null != hash) {
                Object tmp=hash.remove(hfield);
                if(null != tmp) {// 如果删除成功
                    this.changeListener.cacheChange();
                }
            }
        }

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

    public boolean isNeedPersist() {
        return needPersist;
    }

    public void setNeedPersist(boolean needPersist) {
        this.needPersist=needPersist;
    }

    public int getUnpersistMaxSize() {
        return unpersistMaxSize;
    }

    public void setUnpersistMaxSize(int unpersistMaxSize) {
        if(unpersistMaxSize > 0) {
            this.unpersistMaxSize=unpersistMaxSize;
        }
    }

    public boolean isCopyValueOnGet() {
        return copyValueOnGet;
    }

    public void setCopyValueOnGet(boolean copyValueOnGet) {
        this.copyValueOnGet=copyValueOnGet;
    }

    public boolean isCopyValueOnSet() {
        return copyValueOnSet;
    }

    public void setCopyValueOnSet(boolean copyValueOnSet) {
        this.copyValueOnSet=copyValueOnSet;
    }

    public int getClearAndPersistPeriod() {
        return clearAndPersistPeriod;
    }

    public void setClearAndPersistPeriod(int clearAndPersistPeriod) {
        this.clearAndPersistPeriod=clearAndPersistPeriod;
    }

    @Override
    public ICloner getCloner() {
        return this.cloner;
    }

    @Override
    public ISerializer<Object> getSerializer() {
        return this.serializer;
    }

    @Override
    public AutoLoadConfig getAutoLoadConfig() {
        return this.config;
    }

}
