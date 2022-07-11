
package com.jarvis.cache.admin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jarvis.cache.CacheHandler;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/autoload-cache")
public class AutoloadCacheController {

    private final CacheHandler autoloadCacheHandler;

    public AutoloadCacheController(CacheHandler autoloadCacheHandler) {
        this.autoloadCacheHandler = autoloadCacheHandler;
    }

    @GetMapping
    public AutoLoadVO[] listAutoLoadVO() {
        AutoLoadTO queue[] = autoloadCacheHandler.getAutoLoadHandler().getAutoLoadQueue();
        if (null == queue || queue.length == 0) {
            return null;
        }
        AutoLoadVO[] autoLoadVOs = new AutoLoadVO[queue.length];
        for (int i = 0; i < queue.length; i++) {
            AutoLoadTO tmpTO = queue[i];
            CacheAopProxyChain pjp = tmpTO.getJoinPoint();
            String className = pjp.getTarget().getClass().getName();
            String methodName = pjp.getMethod().getName();
            CacheKeyTO cacheKeyTO = tmpTO.getCacheKey();
            AutoLoadVO autoLoadVO = new AutoLoadVO();
            autoLoadVO.setNamespace(cacheKeyTO.getNamespace());
            autoLoadVO.setKey(cacheKeyTO.getKey());
            autoLoadVO.setHfield(cacheKeyTO.getHfield());
            autoLoadVO.setMethod(className + "." + methodName);
            autoLoadVO.setLastRequestTime(formatDate(tmpTO.getLastRequestTime()));
            autoLoadVO.setFirstRequestTime(formatDate(tmpTO.getFirstRequestTime()));
            autoLoadVO.setRequestTimes(tmpTO.getRequestTimes());
            autoLoadVO.setLastLoadTime(formatDate(tmpTO.getLastLoadTime()));
            autoLoadVO.setExpire(tmpTO.getCache().expire());
            // 缓存过期时间
            autoLoadVO.setExpireTime(formatDate(tmpTO.getLastLoadTime() + tmpTO.getCache().expire() * 1000));
            autoLoadVO.setRequestTimeout(tmpTO.getCache().requestTimeout());
            autoLoadVO.setRequestTimeoutTime(
                    formatDate(tmpTO.getLastRequestTime() + tmpTO.getCache().requestTimeout() * 1000));
            autoLoadVO.setLoadCount(tmpTO.getLoadCnt());
            autoLoadVO.setAverageUseTime(tmpTO.getAverageUseTime());

            autoLoadVOs[i] = autoLoadVO;
        }
        return autoLoadVOs;
    }

    @GetMapping("/args")
    public Object[] showArgs(String key, String hfield) {
        CacheKeyTO cacheKeyTO = new CacheKeyTO(autoloadCacheHandler.getAutoLoadConfig().getNamespace(), key, hfield);
        AutoLoadTO tmpTO = autoloadCacheHandler.getAutoLoadHandler().getAutoLoadTO(cacheKeyTO);
        if (null != tmpTO && null != tmpTO.getArgs()) {
            return tmpTO.getArgs();
        }
        return null;
    }

    @PostMapping("removeCache")
    public boolean removeCache(String key, String hfield) {
        CacheKeyTO cacheKeyTO = new CacheKeyTO(autoloadCacheHandler.getAutoLoadConfig().getNamespace(), key, hfield);
        try {
            Set<CacheKeyTO> keys=new HashSet<>();
            keys.add(cacheKeyTO);
            autoloadCacheHandler.delete(keys);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @PostMapping("removeAutoloadTO")
    public boolean removeAutoloadTO(String key, String hfield) {
        CacheKeyTO cacheKeyTO = new CacheKeyTO(autoloadCacheHandler.getAutoLoadConfig().getNamespace(), key, hfield);
        try {
            autoloadCacheHandler.getAutoLoadHandler().removeAutoLoadTO(cacheKeyTO);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @PostMapping("resetLastLoadTime")
    public boolean resetLastLoadTime(String key, String hfield) {
        CacheKeyTO cacheKeyTO = new CacheKeyTO(autoloadCacheHandler.getAutoLoadConfig().getNamespace(), key, hfield);
        try {
            autoloadCacheHandler.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKeyTO);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private static final ThreadLocal<SimpleDateFormat> FORMATER = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }

    };

    private String formatDate(long time) {
        if (time < 100000) {
            return "";
        }
        Date date = new Date(time);
        return FORMATER.get().format(date);
    }
}
