
package com.jarvis.cache.admin;

import lombok.Data;

@Data
public class AutoLoadVO {

    private String namespace;

    private String key;

    private String hfield;

    private String method;

    private String firstRequestTime;

    private String lastRequestTime;

    private long requestTimes;

    private long expire;

    private String expireTime;

    private long requestTimeout;

    private String requestTimeoutTime;

    private String lastLoadTime;

    private long loadCount;

    private long averageUseTime;
}
