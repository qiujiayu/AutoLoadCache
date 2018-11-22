package com.jarvis.cache;

import java.util.HashSet;
import java.util.Set;

import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.type.CacheOpType;

/**
 * @author: jiayu.qiu
 */
public class CacheHelper {

    private static final ThreadLocal<CacheOpType> OP_TYPE = new ThreadLocal<CacheOpType>();

    private static final ThreadLocal<Set<CacheKeyTO>> DELETE_CACHE_KEYS = new ThreadLocal<Set<CacheKeyTO>>();

    /**
     * 获取CacheOpType
     * 
     * @return ThreadLocal中设置的CacheOpType
     */
    public static CacheOpType getCacheOpType() {
        return OP_TYPE.get();
    }

    /**
     * 设置CacheOpType
     * 
     * @param opType CacheOpType
     */
    public static void setCacheOpType(CacheOpType opType) {
        OP_TYPE.set(opType);
    }

    /**
     * 移除CacheOpType
     */
    public static void clearCacheOpType() {
        OP_TYPE.remove();
    }

    public static void initDeleteCacheKeysSet() {
        Set<CacheKeyTO> set = DELETE_CACHE_KEYS.get();
        if (null == set) {
            set = new HashSet<CacheKeyTO>();
            DELETE_CACHE_KEYS.set(set);
        }
    }

    public static Set<CacheKeyTO> getDeleteCacheKeysSet() {
        return DELETE_CACHE_KEYS.get();
    }

    public static void addDeleteCacheKey(CacheKeyTO key) {
        Set<CacheKeyTO> set = DELETE_CACHE_KEYS.get();
        if (null != set) {
            set.add(key);
        }
    }

    public static boolean isOnTransactional(){
        Set<CacheKeyTO> set = DELETE_CACHE_KEYS.get();
        return null != set;
    }

    public static void clearDeleteCacheKeysSet() {
        DELETE_CACHE_KEYS.remove();
    }
}
