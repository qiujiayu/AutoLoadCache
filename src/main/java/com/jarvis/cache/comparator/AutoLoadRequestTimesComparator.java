package com.jarvis.cache.comparator;

import java.util.Comparator;

import com.jarvis.cache.to.AutoLoadTO;

/**
 * 根据请求次数，倒序排序，请求次数越多，说明使用频率越高，造成并发的可能越大。
 * 
 * @author jiayu.qiu
 */
public class AutoLoadRequestTimesComparator implements Comparator<AutoLoadTO> {

    @Override
    public int compare(AutoLoadTO autoLoadTO1, AutoLoadTO autoLoadTO2) {
        if (autoLoadTO1 == null && autoLoadTO2 != null) {
            return 1;
        } else if (autoLoadTO1 != null && autoLoadTO2 == null) {
            return -1;
        } else if (autoLoadTO1 == null && autoLoadTO2 == null) {
            return 0;
        }
        if (autoLoadTO1.getRequestTimes() > autoLoadTO2.getRequestTimes()) {
            return -1;
        } else if (autoLoadTO1.getRequestTimes() < autoLoadTO2.getRequestTimes()) {
            return 1;
        }
        return 0;
    }

}
