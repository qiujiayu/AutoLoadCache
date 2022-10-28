package com.jarvis.cache.to;


/**
 *
 */
public class RedisLockInfo {

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 租约时长
     */
    private Integer leaseTime;

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Integer getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(Integer leaseTime) {
        this.leaseTime = leaseTime;
    }
}
