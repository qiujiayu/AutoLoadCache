package com.jarvis.cache.to;

import lombok.Data;

/**
 *
 */
@Data
public class RedisLockInfo {

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 租约时长
     */
    private Integer leaseTime;

}
