package com.jarvis.cache.type;

import java.util.Comparator;

import com.jarvis.cache.comparator.AutoLoadOldestComparator;
import com.jarvis.cache.comparator.AutoLoadRequestTimesComparator;
import com.jarvis.cache.to.AutoLoadTO;

/**
 * @author: jiayu.qiu
 */
public enum AutoLoadQueueSortType {
    /**
     * 默认顺序
     */
    NONE(0, null),
    /**
     * 越接近过期时间，越耗时的排在最前
     */
    OLDEST_FIRST(1, new AutoLoadOldestComparator()),

    /**
     * 根据请求次数，倒序排序，请求次数越多，说明使用频率越高，造成并发的可能越大。
     */
    REQUEST_TIMES_DESC(2, new AutoLoadRequestTimesComparator());

    private Integer id;

    private Comparator<AutoLoadTO> comparator;

    private AutoLoadQueueSortType(Integer id, Comparator<AutoLoadTO> comparator) {
        this.id = id;
        this.comparator = comparator;
    }

    public static AutoLoadQueueSortType getById(Integer id) {
        if (null == id) {
            return NONE;
        }
        AutoLoadQueueSortType[] values = AutoLoadQueueSortType.values();
        for (AutoLoadQueueSortType tmp : values) {
            if (id.intValue() == tmp.getId().intValue()) {
                return tmp;
            }
        }
        return NONE;
    }

    public Integer getId() {
        return id;
    }

    public Comparator<AutoLoadTO> getComparator() {
        return comparator;
    }

}
