package com.jarvis.cache;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.annotation.ExCache;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.aop.DeleteCacheAopProxyChain;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.to.ProcessingTO;
import com.jarvis.cache.type.CacheOpType;

/**
 * 缓存管理抽象类
 * @author jiayu.qiu
 */
public abstract class AbstractCacheManager implements ICacheManager {

    private static final Logger logger=Logger.getLogger(AbstractCacheManager.class);

    // 解决java.lang.NoSuchMethodError:java.util.Map.putIfAbsent
    public final ConcurrentHashMap<CacheKeyTO, ProcessingTO> processing=new ConcurrentHashMap<CacheKeyTO, ProcessingTO>();

    private final AutoLoadHandler autoLoadHandler;

    private String namespace;

    /**
     * 序列化工具，默认使用Hessian2
     */
    private final ISerializer<Object> serializer;

    /**
     * 表达式解析器
     */
    private final AbstractScriptParser scriptParser;

    private final RefreshHandler refreshHandler;

    private ICloner cloner;

    public AbstractCacheManager(AutoLoadConfig config, ISerializer<Object> serializer, AbstractScriptParser scriptParser) {
        autoLoadHandler=new AutoLoadHandler(this, config);
        this.serializer=serializer;
        this.cloner=this.serializer;
        this.scriptParser=scriptParser;
        registerFunction(config.getFunctions());
        refreshHandler=new RefreshHandler(this, config);
    }

    /**
     * 处理@Cache 拦截
     * @param pjp 切面
     * @param cache 注解
     * @return T 返回值
     * @throws Exception 异常
     */
    public Object proceed(CacheAopProxyChain pjp, Cache cache) throws Throwable {
        Object[] arguments=pjp.getArgs();
        if(null != cache.opType() && cache.opType() == CacheOpType.WRITE) {// 更新缓存操作
            DataLoader dataLoader=new DataLoader(pjp, cache, this);
            Object result=dataLoader.getData();
            CacheWrapper<Object> cacheWrapper=dataLoader.buildCacheWrapper(result).getCacheWrapper();
            if(scriptParser.isCacheable(cache, arguments, result)) {
                CacheKeyTO cacheKey=getCacheKey(pjp, cache, result);
                AutoLoadTO autoLoadTO=autoLoadHandler.getAutoLoadTO(cacheKey);// 注意：这里只能获取AutoloadTO，不能生成AutoloadTO
                try {
                    writeCache(pjp, pjp.getArgs(), cache, cacheKey, cacheWrapper);
                    if(null != autoLoadTO) {
                        autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime())// 同步加载时间
                            .setExpire(cacheWrapper.getExpire());// 同步过期时间
                    }
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            return result;
        }
        if(!scriptParser.isCacheable(cache, arguments)) {// 如果不进行缓存，则直接返回数据
            return getData(pjp);
        }
        CacheKeyTO cacheKey=getCacheKey(pjp, cache);
        if(null == cacheKey) {
            return getData(pjp);
        }
        Method method=pjp.getMethod();
        // Type returnType=method.getGenericReturnType();
        CacheWrapper<Object> cacheWrapper=null;
        try {
            cacheWrapper=this.get(cacheKey, method, arguments);// 从缓存中获取数据
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if(null != cache.opType() && cache.opType() == CacheOpType.READ_ONLY) {// 如果是只读，则直接返回
            return null == cacheWrapper ? null : cacheWrapper.getCacheObject();
        }
        if(null != cacheWrapper && !cacheWrapper.isExpired()) {
            AutoLoadTO autoLoadTO=autoLoadHandler.putIfAbsent(cacheKey, pjp, cache, cacheWrapper);
            if(null != autoLoadTO) {// 同步最后加载时间
                autoLoadTO.setLastRequestTime(System.currentTimeMillis())//
                    .setLastLoadTime(cacheWrapper.getLastLoadTime())// 同步加载时间
                    .setExpire(cacheWrapper.getExpire());// 同步过期时间
            } else {// 如果缓存快要失效，则自动刷新
                refreshHandler.doRefresh(pjp, cache, cacheKey, cacheWrapper);
            }
            return cacheWrapper.getCacheObject();
        }
        DataLoader dataLoader=new DataLoader(pjp, cacheKey, cache, this);
        CacheWrapper<Object> newCacheWrapper=dataLoader.loadData().getCacheWrapper();
        AutoLoadTO autoLoadTO=null;
        if(dataLoader.isFirst()) {
            autoLoadTO=autoLoadHandler.putIfAbsent(cacheKey, pjp, cache, newCacheWrapper);
            try {
                writeCache(pjp, pjp.getArgs(), cache, cacheKey, newCacheWrapper);
                if(null != autoLoadTO) {// 同步最后加载时间
                    autoLoadTO.setLastRequestTime(System.currentTimeMillis())//
                        .setLastLoadTime(newCacheWrapper.getLastLoadTime())// 同步加载时间
                        .setExpire(newCacheWrapper.getExpire())// 同步过期时间
                        .addUseTotalTime(dataLoader.getLoadDataUseTime());// 统计用时
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return newCacheWrapper.getCacheObject();
    }

    /**
     * 处理@CacheDelete 拦截
     * @param jp 切点
     * @param cacheDelete 拦截到的注解
     * @param retVal 返回值
     */
    public void deleteCache(DeleteCacheAopProxyChain jp, CacheDelete cacheDelete, Object retVal) {
        Object[] arguments=jp.getArgs();
        CacheDeleteKey[] keys=cacheDelete.value();
        if(null == keys || keys.length == 0) {
            return;
        }
        for(int i=0; i < keys.length; i++) {
            CacheDeleteKey keyConfig=keys[i];
            try {
                if(!scriptParser.isCanDelete(keyConfig, arguments, retVal)) {
                    continue;
                }
                CacheKeyTO key=getCacheKey(jp, keyConfig, retVal);
                if(null != key) {
                    this.delete(key);
                }
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 直接加载数据（加载后的数据不往缓存放）
     * @param pjp CacheAopProxyChain
     * @return Object
     * @throws Throwable
     */
    private Object getData(CacheAopProxyChain pjp) throws Throwable {
        try {
            long startTime=System.currentTimeMillis();
            Object[] arguments=pjp.getArgs();
            Object result=pjp.doProxyChain(arguments);
            long useTime=System.currentTimeMillis() - startTime;
            AutoLoadConfig config=autoLoadHandler.getConfig();
            if(config.isPrintSlowLog() && useTime >= config.getSlowLoadTime()) {
                String className=pjp.getTargetClass().getName();
                logger.error(className + "." + pjp.getMethod().getName() + ", use time:" + useTime + "ms");
            }
            return result;
        } catch(Throwable e) {
            throw e;
        }
    }

    public void writeCache(CacheAopProxyChain pjp, Object[] arguments, Cache cache, CacheKeyTO cacheKey, CacheWrapper<Object> cacheWrapper) throws Exception {
        if(null == cacheKey) {
            return;
        }
        Method method=pjp.getMethod();
        this.setCache(cacheKey, cacheWrapper, method, arguments);
        ExCache[] exCaches=cache.exCache();
        if(null == exCaches || exCaches.length == 0) {
            return;
        }

        Object result=cacheWrapper.getCacheObject();
        for(ExCache exCache: exCaches) {
            try {
                if(!scriptParser.isCacheable(exCache, arguments, result)) {
                    continue;
                }
                CacheKeyTO exCacheKey=getCacheKey(pjp, arguments, exCache, result);
                if(null == exCacheKey) {
                    continue;
                }
                Object exResult=null;
                if(null == exCache.cacheObject() || exCache.cacheObject().length() == 0) {
                    exResult=result;
                } else {
                    exResult=scriptParser.getElValue(exCache.cacheObject(), arguments, result, true, Object.class);
                }

                int exCacheExpire=scriptParser.getRealExpire(exCache.expire(), exCache.expireExpression(), arguments, exResult);
                CacheWrapper<Object> exCacheWrapper=new CacheWrapper<Object>(exResult, exCacheExpire);
                AutoLoadTO tmpAutoLoadTO=this.autoLoadHandler.getAutoLoadTO(exCacheKey);
                this.setCache(exCacheKey, exCacheWrapper, method, arguments);
                if(null != tmpAutoLoadTO) {
                    tmpAutoLoadTO.setExpire(exCacheExpire)//
                        .setLastLoadTime(exCacheWrapper.getLastLoadTime());//
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

    }

    public void destroy() {
        autoLoadHandler.shutdown();
        refreshHandler.shutdown();
        logger.info("cache destroy ... ... ...");
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
    private CacheKeyTO getCacheKey(String className, String methodName, Object[] arguments, String _key, String _hfield, Object result, boolean hasRetVal) {
        String key=null;
        String hfield=null;
        if(null != _key && _key.trim().length() > 0) {
            try {
                key=scriptParser.getDefinedCacheKey(_key, arguments, result, hasRetVal);
                if(null != _hfield && _hfield.trim().length() > 0) {
                    hfield=scriptParser.getDefinedCacheKey(_hfield, arguments, result, hasRetVal);
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            key=CacheUtil.getDefaultCacheKey(className, methodName, arguments);
        }
        if(null == key || key.trim().length() == 0) {
            logger.error(className + "." + methodName + "; cache key is empty");
            return null;
        }
        return new CacheKeyTO(namespace, key, hfield);
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @return String 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache) {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        Object[] arguments=pjp.getArgs();
        String _key=cache.key();
        String _hfield=cache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, null, false);
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param cache
     * @param result 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache, Object result) {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        Object[] arguments=pjp.getArgs();
        String _key=cache.key();
        String _hfield=cache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, result, true);
    }

    /**
     * 生成缓存 Key
     * @param pjp
     * @param arguments
     * @param exCache
     * @param result 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Object[] arguments, ExCache exCache, Object result) {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        String _key=exCache.key();
        if(null == _key || _key.trim().length() == 0) {
            return null;
        }
        String _hfield=exCache.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, result, true);
    }

    /**
     * 生成缓存 Key
     * @param jp
     * @param cacheDeleteKey
     * @param retVal 执行结果值
     * @return 缓存Key
     * @throws Exception
     */
    private CacheKeyTO getCacheKey(DeleteCacheAopProxyChain jp, CacheDeleteKey cacheDeleteKey, Object retVal) {
        String className=jp.getTargetClass().getName();
        String methodName=jp.getMethod().getName();
        Object[] arguments=jp.getArgs();
        String _key=cacheDeleteKey.value();
        String _hfield=cacheDeleteKey.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, retVal, true);

    }

    public ISerializer<Object> getSerializer() {
        return serializer;
    }

    public AutoLoadHandler getAutoLoadHandler() {
        return this.autoLoadHandler;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace=namespace;
    }

    public AbstractScriptParser getScriptParser() {
        return scriptParser;
    }

    public ICloner getCloner() {
        return cloner;
    }

    public void setCloner(ICloner cloner) {
        this.cloner=cloner;
    }

    private void registerFunction(Map<String, String> funcs) {
        if(null == scriptParser) {
            return;
        }
        if(null == funcs || funcs.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, String>> it=funcs.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, String> entry=it.next();
            try {
                String name=entry.getKey();
                Class<?> cls=Class.forName(entry.getValue());
                Method method=cls.getDeclaredMethod(name, new Class[]{Object.class});
                scriptParser.addFunction(name, method);
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
