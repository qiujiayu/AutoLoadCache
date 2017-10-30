package com.jarvis.cache.to;

import lombok.Data;

/**
 * @author jiayu.qiu
 */
@Data
public class RedisLockInfo {

    private Long startTime;

    private Integer leaseTime;

}
