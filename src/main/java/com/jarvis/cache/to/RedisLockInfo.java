package com.jarvis.cache.to;

/**
 * @author jiayu.qiu
 */
public class RedisLockInfo {

    private Long startTime;

    private Integer leaseTime;

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime=startTime;
    }

    public Integer getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(Integer leaseTime) {
        this.leaseTime=leaseTime;
    }
}
