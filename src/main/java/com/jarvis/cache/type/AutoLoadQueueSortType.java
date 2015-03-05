package com.jarvis.cache.type;

public enum AutoLoadQueueSortType {
    /**
     * 默认顺序
     */
    NONE,
    /**
     * 越接近过期时间，越耗时的排在最前
     */
    OLDEST_FIRST;
}
