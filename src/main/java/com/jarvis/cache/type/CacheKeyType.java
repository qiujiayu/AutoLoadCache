package com.jarvis.cache.type;

import java.io.Serializable;

public enum CacheKeyType implements Serializable {
    DEFAULT, // 默认生成的Key
        DEFINED // 自定交的Key
    ;
}
