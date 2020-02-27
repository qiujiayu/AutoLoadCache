package com.jarvis.cache.autoconfigure;

import com.jarvis.cache.lock.ILock;
import com.jarvis.cache.redis.SpringRedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 对分布式锁进行一些默认配置<br>
 * 如果需要自定义，需要自行覆盖即可
 *
 * @author: jiayu.qiu
 */
@Configuration
@AutoConfigureAfter({AutoloadCacheManageConfiguration.class})
public class DistributedLockConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockConfiguration.class);

    @Bean
    @ConditionalOnMissingBean({ILock.class})
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public ILock autoLoadCacheDistributedLock(RedisConnectionFactory connectionFactory) {
        if (null == connectionFactory) {
            return null;
        }

        SpringRedisLock lock = new SpringRedisLock(connectionFactory);
        if (logger.isDebugEnabled()) {
            logger.debug("ILock with SpringJedisLock auto-configured");
        }
        return lock;
    }

}
