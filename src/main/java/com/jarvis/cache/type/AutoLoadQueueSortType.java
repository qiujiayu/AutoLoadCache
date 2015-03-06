package com.jarvis.cache.type;

public enum AutoLoadQueueSortType {
    /**
     * 默认顺序
     */
    NONE(0),
    /**
     * 越接近过期时间，越耗时的排在最前
     */
    OLDEST_FIRST(1);

    private Integer id;

    private AutoLoadQueueSortType(Integer id) {
        this.id=id;
    }

    public static AutoLoadQueueSortType getById(Integer id) {
        if(null == id) {
            return NONE;
        }
        AutoLoadQueueSortType values[]=AutoLoadQueueSortType.values();
        for(AutoLoadQueueSortType tmp: values) {
            if(id.intValue() == tmp.getId().intValue()) {
                return tmp;
            }
        }
        return NONE;
    }

    public Integer getId() {
        return id;
    }
}
