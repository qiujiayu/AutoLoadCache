package com.jarvis.cache.type;

/**
 * 缓存操作类型
 * @author jiayu.qiu
 */
public enum CacheOpType {
        /**
         * 读写缓存操
         */
    READ_WRITE, /**
                 * 只往缓存写数据，不从缓存中读数据
                 */
    WRITE, /**
            * 只从缓存中读取，用于其它地方往缓存写，这里只读的场景。
            */
    READ_ONLY;
}
