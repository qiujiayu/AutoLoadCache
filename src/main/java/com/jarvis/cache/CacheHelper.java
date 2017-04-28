package com.jarvis.cache;

import java.util.HashSet;
import java.util.Set;

import com.jarvis.cache.to.CacheConfigTO;
import com.jarvis.cache.to.CacheKeyTO;

public class CacheHelper {

    private static final ThreadLocal<CacheConfigTO> CONFIG=new ThreadLocal<CacheConfigTO>();

    private static final ThreadLocal<Set<CacheKeyTO>> DELETE_CACHE_KEYS=new ThreadLocal<Set<CacheKeyTO>>();

    public static CacheConfigTO getLocalConfig() {
        return CONFIG.get();
    }

    private static void setLocalConfig(CacheConfigTO config) {
        CONFIG.set(config);
    }

    /**
     * 移除本地变量
     */
    public static void clearLocalConfig() {
        CONFIG.remove();
    }

    public static CacheConfigTO setCacheAble(boolean cacheAble) {
        CacheConfigTO config=getLocalConfig();
        if(null == config) {
            config=new CacheConfigTO();
        }
        config.setCacheAble(cacheAble);
        setLocalConfig(config);
        return config;
    }

    public static void initDeleteCacheKeysSet() {
        Set<CacheKeyTO> set=DELETE_CACHE_KEYS.get();
        if(null == set) {
            set=new HashSet<CacheKeyTO>();
            DELETE_CACHE_KEYS.set(set);
        }
    }

    public static Set<CacheKeyTO> getDeleteCacheKeysSet() {
        return DELETE_CACHE_KEYS.get();
    }

    public static boolean addDeleteCacheKey(CacheKeyTO key) {
        Set<CacheKeyTO> set=DELETE_CACHE_KEYS.get();
        if(null != set) {
            set.add(key);
            return true;
        }
        return false;
    }
}
