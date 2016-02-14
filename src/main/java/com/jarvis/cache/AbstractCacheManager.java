package com.jarvis.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.type.CacheOpType;

/**
 * 缓存管理抽象类
 * @author jiayu.qiu
 */
public abstract class AbstractCacheManager implements ICacheManager {

    private static final Logger logger=Logger.getLogger(AbstractCacheManager.class);

    // 解决java.lang.NoSuchMethodError:java.util.Map.putIfAbsent
    private final ConcurrentHashMap<String, Boolean> processing=new ConcurrentHashMap<String, Boolean>();

    private AutoLoadHandler autoLoadHandler;

    private String namespace;

    /**
     * 序列化工具，默认使用Hessian2
     */
    private ISerializer<Object> serializer=new HessianSerializer();

    public AbstractCacheManager(AutoLoadConfig config) {
        autoLoadHandler=new AutoLoadHandler(this, config);
    }

    public ISerializer<Object> getSerializer() {
        return serializer;
    }

    public void setSerializer(ISerializer<Object> serializer) {
        this.serializer=serializer;
    }

    @Override
    public AutoLoadHandler getAutoLoadHandler() {
        return this.autoLoadHandler;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace=namespace;
    }

    /**
     * 生成缓存KeyTO
     * @param className 类名
     * @param methodName 方法名
     * @param arguments 参数
     * @param _key key
     * @param _hfield hfield
     * @param result 执行实际方法的返回值
     * @return CacheKeyTO
     */
    private CacheKeyTO getCacheKey(String className, String methodName, Object[] arguments, String _key, String _hfield,
        Object result) {
        String key=null;
        String hfield=null;
        if(null != _key && _key.trim().length() > 0) {
            key=CacheUtil.getDefinedCacheKey(_key, arguments, result);
            if(null != _hfield && _hfield.trim().length() > 0) {
                hfield=CacheUtil.getDefinedCacheKey(_hfield, arguments, result);
            }
        } else {
            key=CacheUtil.getDefaultCacheKey(className, methodName, arguments);
        }
        if(null == key || key.trim().length() == 0) {
            logger.error(className + "." + methodName + "; cache key is empty");
            return null;
        }
        CacheKeyTO to=new CacheKeyTO();
        to.setNamespace(namespace);
        to.setKey(key);
        to.setHfield(hfield);
        return to;
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @return String 缓存Key
     */
    private CacheKeyTO getCacheKey(ProceedingJoinPoint pjp, Cache cache) {
        String className=pjp.getTarget().getClass().getName();
        String methodName=pjp.getSignature().getName();
        Object[] arguments=pjp.getArgs();
        String _key=cache.key();
        String _hfield=cache.hfield();

        return getCacheKey(className, methodName, arguments, _key, _hfield, null);
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @param result 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(ProceedingJoinPoint pjp, Cache cache, Object result) {
        String className=pjp.getTarget().getClass().getName();
        String methodName=pjp.getSignature().getName();
        Object[] arguments=pjp.getArgs();
        String _key=cache.key();
        String _hfield=cache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, result);
    }

    /**
     * 生成缓存 Key
     * @param jp
     * @param cacheDeleteKey
     * @param retVal 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(JoinPoint jp, CacheDeleteKey cacheDeleteKey, Object retVal) {
        String className=jp.getTarget().getClass().getName();
        String methodName=jp.getSignature().getName();
        Object[] arguments=jp.getArgs();
        String _key=cacheDeleteKey.value();
        String _hfield=cacheDeleteKey.hfield();

        return getCacheKey(className, methodName, arguments, _key, _hfield, retVal);

    }

    /**
     * 处理@Cache 拦截
     * @param pjp 切面
     * @param cache 注解
     * @return T 返回值
     * @throws Exception 异常
     */
    public Object proceed(ProceedingJoinPoint pjp, Cache cache) throws Exception {
        Object[] arguments=pjp.getArgs();
        // Signature signature=pjp.getSignature();
        // MethodSignature methodSignature=(MethodSignature)signature;
        // Class returnType=methodSignature.getReturnType(); // 获取返回值类型
        // System.out.println("returnType:" + returnType.getName());
        int expire=cache.expire();
        if(null != cache.opType() && cache.opType() == CacheOpType.WRITE) {// 更新缓存操作
            Object result=getData(pjp, null);
            if(CacheUtil.isCacheable(cache, arguments, result)) {
                CacheKeyTO cacheKey=getCacheKey(pjp, cache, result);
                writeCache(result, cacheKey, expire);
            }
            return result;
        }
        if(!CacheUtil.isCacheable(cache, arguments)) {// 如果不进行缓存，则直接返回数据
            return getData(pjp, null);
        }
        CacheKeyTO cacheKey=getCacheKey(pjp, cache);
        if(null == cacheKey) {
            return getData(pjp, null);
        }
        AutoLoadTO autoLoadTO=null;
        if(CacheUtil.isAutoload(cache, arguments)) {
            try {
                autoLoadTO=autoLoadHandler.getAutoLoadTO(cacheKey);
                if(null == autoLoadTO) {
                    AutoLoadTO tmp=autoLoadHandler.putIfAbsent(cacheKey, pjp, expire, cache.requestTimeout(), serializer);
                    if(null != tmp) {
                        autoLoadTO=tmp;
                    }
                }
                autoLoadTO.setLastRequestTime(System.currentTimeMillis());
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        CacheWrapper cacheWrapper=this.get(cacheKey);
        if(null != cacheWrapper && !cacheWrapper.isExpired()) {
            if(null != autoLoadTO && cacheWrapper.getLastLoadTime() > autoLoadTO.getLastLoadTime()) {// 同步最后加载时间
                autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime());
            }
            return cacheWrapper.getCacheObject();
        }
        return loadData(pjp, autoLoadTO, cacheKey, cache);
    }

    /**
     * 写缓存
     * @param result 缓存数据
     * @param cacheKey 缓存Key
     * @param expire 缓存时间
     */
    private void writeCache(Object result, CacheKeyTO cacheKey, int expire) {
        if(null == cacheKey) {
            return;
        }
        CacheWrapper tmp=new CacheWrapper(result, expire);
        this.setCache(cacheKey, tmp);
    }

    /**
     * 通过ProceedingJoinPoint加载数据
     * @param pjp
     * @param autoLoadTO
     * @param cacheKey
     * @param cache
     * @return 返回值
     * @throws Exception
     */
    private Object loadData(ProceedingJoinPoint pjp, AutoLoadTO autoLoadTO, CacheKeyTO cacheKey, Cache cache) throws Exception {
        Boolean isProcessing=processing.get(cacheKey);
        if(null == isProcessing) {
            Boolean _isProcessing=processing.putIfAbsent(cacheKey.getFullKey(), Boolean.TRUE);// 为发减少数据层的并发，增加等待机制。
            if(null != _isProcessing) {
                isProcessing=_isProcessing;
            }
        }
        int expire=cache.expire();
        Object target=pjp.getTarget();
        Object result=null;
        try {
            if(null == isProcessing) {
                result=getData(pjp, autoLoadTO);
            } else {
                long startWait=System.currentTimeMillis();
                while(System.currentTimeMillis() - startWait < cache.waitTimeOut()) {// 等待
                    if(null == processing.get(cacheKey)) {// 防止频繁去缓存取数据，造成缓存服务器压力过大
                        CacheWrapper cacheWrapper=this.get(cacheKey);
                        if(cacheWrapper != null && !cacheWrapper.isExpired()) {
                            return cacheWrapper.getCacheObject();
                        }
                        break;// 如果上个请求已经出异常，则需要跳出
                    } else {
                        synchronized(target) {
                            try {
                                target.wait(10);
                            } catch(InterruptedException ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }
                result=getData(pjp, autoLoadTO);
            }
        } catch(Exception e) {
            throw e;
        } catch(Throwable e) {
            throw new Exception(e);
        } finally {
            processing.remove(cacheKey);
            synchronized(target) {
                target.notifyAll();
            }
        }
        writeCache(result, cacheKey, expire);
        return result;
    }

    private Object getData(ProceedingJoinPoint pjp, AutoLoadTO autoLoadTO) throws Exception {
        try {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(true);
            }
            long startTime=System.currentTimeMillis();
            Object result=pjp.proceed();
            long useTime=System.currentTimeMillis() - startTime;
            AutoLoadConfig config=autoLoadHandler.getConfig();
            if(config.isPrintSlowLog() && useTime >= config.getSlowLoadTime()) {
                String className=pjp.getTarget().getClass().getName();
                logger.error(className + "." + pjp.getSignature().getName() + ", use time:" + useTime + "ms");
            }
            if(null != autoLoadTO) {
                autoLoadTO.setLastLoadTime(startTime);
                autoLoadTO.addUseTotalTime(useTime);
            }
            return result;
        } catch(Exception e) {
            throw e;
        } catch(Throwable e) {
            throw new Exception(e);
        } finally {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(false);
            }
        }
    }

    /**
     * 处理@CacheDelete 拦截
     * @param jp 切点
     * @param cacheDelete 拦截到的注解
     * @param retVal 返回值
     */
    public void deleteCache(JoinPoint jp, CacheDelete cacheDelete, Object retVal) {
        Object[] arguments=jp.getArgs();
        CacheDeleteKey[] keys=cacheDelete.value();
        if(null == keys || keys.length == 0) {
            return;
        }
        for(int i=0; i < keys.length; i++) {
            CacheDeleteKey keyConfig=keys[i];
            if(!CacheUtil.isCanDelete(keyConfig, arguments, retVal)) {
                continue;
            }
            CacheKeyTO key=getCacheKey(jp, keyConfig, retVal);
            if(null != key) {
                this.delete(key);
            }
        }
    }

    @Override
    public void destroy() {
        autoLoadHandler.shutdown();
        autoLoadHandler=null;
        logger.info("cache destroy ... ... ...");
    }
}
