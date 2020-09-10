package com.jarvis.cache.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import com.jarvis.cache.to.AutoLoadConfig;

import lombok.Data;

/**
 *
 */
@Data
@ConfigurationProperties(prefix = AutoloadCacheProperties.PREFIX)
public class AutoloadCacheProperties {

    public static final String PREFIX = "autoload.cache";

    private AutoLoadConfig config = new AutoLoadConfig();

    private JedisCacheManagerConfig jedis = new JedisCacheManagerConfig();

    @Autowired
    private Environment env;

    private boolean namespaceEnable = true;

    private boolean proxyTargetClass = true;

    private boolean enable = true;

    /**
     * @Cache 注解是否生效, 默认值为true
     */
    private boolean enableReadAndWrite = true;

    /**
     * @DeleteCache 和 @DeleteCacheTransactional 注解是否生效, 默认值为true
     */
    private boolean enableDelete = true;

    /**
     * @Cache 注解AOP执行顺序
     */
    private int cacheOrder = Integer.MAX_VALUE;

    /**
     * @DeleteCache 注解AOP执行顺序
     */
    private int deleteCacheOrder = Integer.MAX_VALUE;
    /**
     * @DeleteCacheTransactionalAspect 注解AOP执行顺序
     */
    private int deleteCacheTransactionalOrder = 0;

    private String adminUserName = "admin";

    private String adminPassword = "admin";

    @PostConstruct
    public void init() {
        if (namespaceEnable && null != env) {
            String namespace = config.getNamespace();

            if (null == namespace || namespace.trim().length() == 0) {
                String applicationName = env.getProperty("spring.application.name");
                if (null != applicationName && applicationName.trim().length() > 0) {
                    config.setNamespace(applicationName);
                }
            }
        }

    }

    /**
     * 对JedisClusterCacheManager 进行配置
     * 
     *
     */
    @Data
    static class JedisCacheManagerConfig {

        /**
         * Hash的缓存时长：等于0时永久缓存；大于0时，主要是为了防止一些已经不用的缓存占用内存;hashExpire小于0时，则使用@Cache中设置的expire值（默认值为-1）。
         */
        private int hashExpire = -1;

    }
}
