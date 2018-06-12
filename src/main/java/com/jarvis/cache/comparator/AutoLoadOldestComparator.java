package com.jarvis.cache.comparator;

import java.util.Comparator;

import com.jarvis.cache.to.AutoLoadTO;

/**
 * 排序算法：越接近过期时间，越耗时的排在最前,即： System.currentTimeMillis() -
 * autoLoadTO.getLastLoadTime()-autoLoadTO.getExpire()*1000 值越大，排在越前
 * autoLoadTO.getAverageUseTime() 值越大，排在越前
 * 
 * @author jiayu.qiu
 */
public class AutoLoadOldestComparator implements Comparator<AutoLoadTO> {

    @Override
    public int compare(AutoLoadTO autoLoadTO1, AutoLoadTO autoLoadTO2) {
        if (autoLoadTO1 == null && autoLoadTO2 != null) {
            return 1;
        } else if (autoLoadTO1 != null && autoLoadTO2 == null) {
            return -1;
        } else if (autoLoadTO1 == null && autoLoadTO2 == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long dif1 = now - autoLoadTO1.getLastLoadTime() - autoLoadTO1.getCache().expire() * 1000;
        long dif2 = now - autoLoadTO2.getLastLoadTime() - autoLoadTO2.getCache().expire() * 1000;
        if (dif1 > dif2) {
            return -1;
        } else if (dif1 < dif2) {
            return 1;
        } else {
            if (autoLoadTO1.getAverageUseTime() > autoLoadTO2.getAverageUseTime()) {
                return -1;
            } else if (autoLoadTO1.getAverageUseTime() < autoLoadTO2.getAverageUseTime()) {
                return 1;
            }
        }
        return 0;
    }

}
