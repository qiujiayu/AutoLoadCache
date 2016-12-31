package com.jarvis.cache;

import com.jarvis.cache.to.CacheConfigTO;

public class CacheHelper {

    protected static final ThreadLocal<CacheConfigTO> CONFIG=new ThreadLocal<CacheConfigTO>();

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
}
