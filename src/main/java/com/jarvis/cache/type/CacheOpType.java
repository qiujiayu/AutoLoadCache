package com.jarvis.cache.type;

/**
 * 缓存操作类型
 * @author jarvis
 */
public enum CacheOpType {
    /**
     * 读写缓存操
     */
    READ_WRITE,
    /**
     * 只往缓存写数据，不从缓存中读数据
     */
    WRITE;
}
