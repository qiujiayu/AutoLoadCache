package com.jarvis.cache.map;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;

/**
 * 使用ConcurrentHashMap管理缓存
 * @author jiayu.qiu
 */
public class CachePointCut extends AbstractCacheManager {

    private final ConcurrentHashMap<String, Object> cache=new ConcurrentHashMap<String, Object>();

    private CacheChangeListener changeListener;

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
     * 是否拷贝缓存中的值：true时，是拷贝缓存值，可以避免外界修改缓存值；false，不拷贝缓存值，缓存中的数据可能被外界修改，但效率比较高。
     */
    private boolean copyValue=false;

    /**
     * 清除和持久化的时间间隔
     */
    private int clearAndPersistPeriod=60 * 1000; // 1Minutes

    public CachePointCut(AutoLoadConfig config, ISerializer<Object> serializer, AbstractScriptParser scriptParser) {
        super(config, serializer, scriptParser);
        config.setCheckFromCacheBeforeLoad(false);
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

    @Override
    public synchronized void destroy() {
        super.destroy();
        cacheTask.destroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setCache(final CacheKeyTO cacheKeyTO, final CacheWrapper<Object> result, final Method method, final Object args[]) throws CacheCenterConnectionException {
        if(null == cacheKeyTO) {
            return;
        }
        String cacheKey=cacheKeyTO.getCacheKey();
        if(null == cacheKey || cacheKey.length() == 0) {
            return;
        }
        CacheWrapper<Object> value=null;
        if(copyValue) {
            try {
                value=(CacheWrapper<Object>)this.getCloner().deepClone(result, null);// 这里type为null，因为有可以是设置@ExCache缓存
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
            ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>> hash=(ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>>)cache.get(cacheKey);
            if(null == hash) {
                hash=new ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>>();
                ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>> _hash=null;
                _hash=(ConcurrentHashMap<String, SoftReference<CacheWrapper<Object>>>)cache.putIfAbsent(cacheKey, hash);
                if(null != _hash) {
                    hash=_hash;
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
        if(copyValue) {
            try {
                CacheWrapper<Object> res=new CacheWrapper<Object>();
                res.setExpire(value.getExpire());
                res.setLastLoadTime(value.getLastLoadTime());
                res.setCacheObject(this.getCloner().deepClone(value.getCacheObject(), method.getReturnType()));
                return res;
            } catch(Exception e) {
                e.printStackTrace();
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

    public boolean isCopyValue() {
        return copyValue;
    }

    public void setCopyValue(boolean copyValue) {
        this.copyValue=copyValue;
    }

    public int getClearAndPersistPeriod() {
        return clearAndPersistPeriod;
    }

    public void setClearAndPersistPeriod(int clearAndPersistPeriod) {
        this.clearAndPersistPeriod=clearAndPersistPeriod;
    }

}
