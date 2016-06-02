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
import com.jarvis.cache.script.IScriptParser;
import com.jarvis.cache.script.ScriptParserUtil;
import com.jarvis.cache.script.SpringELParser;
import com.jarvis.cache.serializer.HessianSerializer;
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
    private final ConcurrentHashMap<String, ProcessingTO> processing=new ConcurrentHashMap<String, ProcessingTO>();

    private AutoLoadHandler autoLoadHandler;

    private String namespace;

    /**
     * 序列化工具，默认使用Hessian2
     */
    private ISerializer<Object> serializer=new HessianSerializer();

    /**
     * 表达式解析器
     */
    private IScriptParser scriptParser=new SpringELParser();

    private ScriptParserUtil scriptParserUtil=new ScriptParserUtil(scriptParser);

    public AbstractCacheManager(AutoLoadConfig config) {
        autoLoadHandler=new AutoLoadHandler(this, config);
        registerFunction(config.getFunctions());
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

    public IScriptParser getScriptParser() {
        return scriptParser;
    }

    public void setScriptParser(IScriptParser scriptParser) {
        this.scriptParser=scriptParser;
        scriptParserUtil=new ScriptParserUtil(this.scriptParser);
        registerFunction(this.autoLoadHandler.getConfig().getFunctions());
    }

    public ScriptParserUtil getScriptParserUtil() {
        return scriptParserUtil;
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

    /**
     * 处理@Cache 拦截
     * @param pjp 切面
     * @param cache 注解
     * @return T 返回值
     * @throws Exception 异常
     */
    public Object proceed(CacheAopProxyChain pjp, Cache cache) throws Throwable {
        Object[] arguments=pjp.getArgs();
        // Signature signature=pjp.getSignature();
        // MethodSignature methodSignature=(MethodSignature)signature;
        // Class returnType=methodSignature.getReturnType(); // 获取返回值类型
        // System.out.println("returnType:" + returnType.getName());
        if(null != cache.opType() && cache.opType() == CacheOpType.WRITE) {// 更新缓存操作
            CacheWrapper cacheWrapper=getCacheWrapper(pjp, null, cache, null);
            Object result=cacheWrapper.getCacheObject();
            if(scriptParserUtil.isCacheable(cache, arguments, result)) {
                CacheKeyTO cacheKey=getCacheKey(pjp, cache, result);
                writeCache(pjp, null, cache, cacheKey, cacheWrapper);
            }
            return result;
        }
        if(!scriptParserUtil.isCacheable(cache, arguments)) {// 如果不进行缓存，则直接返回数据
            return getData(pjp);
        }
        CacheKeyTO cacheKey=getCacheKey(pjp, cache);
        if(null == cacheKey) {
            return getData(pjp);
        }

        CacheWrapper cacheWrapper=this.get(cacheKey);// 从缓存中获取数据
        if(null != cacheWrapper && !cacheWrapper.isExpired()) {
            AutoLoadTO autoLoadTO=getAutoLoadTO(pjp, arguments, cache, cacheKey, cacheWrapper);
            if(null != autoLoadTO) {// 同步最后加载时间
                autoLoadTO.setLastRequestTime(System.currentTimeMillis());
                autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime());
                autoLoadTO.setExpire(cacheWrapper.getExpire());// 同步过期时间
            }
            return cacheWrapper.getCacheObject();
        }
        return loadData(pjp, null, cacheKey, cache);// 从DAO加载数据
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
                if(!scriptParserUtil.isCanDelete(keyConfig, arguments, retVal)) {
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
     * 通过CacheAopProxyChain加载数据
     * @param pjp CacheAopProxyChain
     * @param autoLoadTO AutoLoadTO
     * @param cacheKey CacheKeyTO
     * @param cache Cache
     * @return 返回值
     * @throws Throwable
     */
    @Override
    public Object loadData(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, CacheKeyTO cacheKey, Cache cache) throws Throwable {
        String fullKey=cacheKey.getFullKey();
        ProcessingTO isProcessing=processing.get(fullKey);
        ProcessingTO processingTO=null;
        if(null == isProcessing) {
            processingTO=new ProcessingTO();
            ProcessingTO _isProcessing=processing.putIfAbsent(fullKey, processingTO);// 为发减少数据层的并发，增加等待机制。
            if(null != _isProcessing) {
                isProcessing=_isProcessing;// 获取到第一个线程的ProcessingTO 的引用，保证所有请求都指向同一个引用
            }
        }
        Object lock=null;
        CacheWrapper cacheWrapper=null;
        // String tname=Thread.currentThread().getName();
        if(null == isProcessing) {// 当前并发中的第一个请求
            lock=processingTO;
            try {
                // System.out.println(tname + " first thread!");
                cacheWrapper=getCacheWrapper(pjp, autoLoadTO, cache, cacheKey);
                writeCache(pjp, autoLoadTO, cache, cacheKey, cacheWrapper);
                processingTO.setCache(cacheWrapper);// 本地缓存
            } catch(Throwable e) {
                processingTO.setError(e);
                throw e;
            } finally {
                processingTO.setFirstFinished(true);
                processing.remove(fullKey);
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
        } else {
            lock=isProcessing;
            long startWait=isProcessing.getStartTime();
            do {// 等待
                if(null == isProcessing) {
                    break;
                }
                if(isProcessing.isFirstFinished()) {
                    cacheWrapper=isProcessing.getCache();// 从本地缓存获取数据， 防止频繁去缓存服务器取数据，造成缓存服务器压力过大
                    // System.out.println(tname + " do FirstFinished" + " is null :" + (null == cacheWrapper));
                    if(null != cacheWrapper) {
                        return cacheWrapper.getCacheObject();
                    }
                    Throwable error=isProcessing.getError();
                    if(null != error) {// 当DAO出错时，直接抛异常
                        // System.out.println(tname + " do error");
                        throw error;
                    }
                    break;
                } else {
                    synchronized(lock) {
                        // System.out.println(tname + " do wait");
                        try {
                            lock.wait(50);// 如果要测试lock对象是否有效，wait时间去掉就可以
                        } catch(InterruptedException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                }
            } while(System.currentTimeMillis() - startWait < cache.waitTimeOut());
            try {
                cacheWrapper=getCacheWrapper(pjp, autoLoadTO, cache, cacheKey);
                writeCache(pjp, autoLoadTO, cache, cacheKey, cacheWrapper);
            } catch(Throwable e) {
                throw e;
            } finally {
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
        }
        if(null != cacheWrapper) {
            return cacheWrapper.getCacheObject();
        }
        return null;
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

    /**
     * 加载数据（加载后的数据需要往缓存放）
     * @param pjp CacheAopProxyChain
     * @param autoLoadTO AutoLoadTO
     * @param cache Cache
     * @param cacheKey CacheKeyTO
     * @return CacheWrapper
     * @throws Throwable
     */
    private CacheWrapper getCacheWrapper(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, Cache cache, CacheKeyTO cacheKey)
        throws Throwable {
        try {
            long startTime=System.currentTimeMillis();
            Object[] arguments;
            if(null == autoLoadTO) {
                arguments=pjp.getArgs();
            } else {
                arguments=autoLoadTO.getArgs();
                autoLoadTO.setLoading(true);
            }
            Object result=pjp.doProxyChain(arguments);
            long useTime=System.currentTimeMillis() - startTime;
            AutoLoadConfig config=autoLoadHandler.getConfig();
            if(config.isPrintSlowLog() && useTime >= config.getSlowLoadTime()) {
                String className=pjp.getTargetClass().getName();
                logger.error(className + "." + pjp.getMethod().getName() + ", use time:" + useTime + "ms");
            }
            int expire=scriptParserUtil.getRealExpire(cache.expire(), cache.expireExpression(), arguments, result);
            CacheWrapper cacheWrapper=new CacheWrapper(result, expire);
            if(null != cacheKey && null == autoLoadTO) {
                autoLoadTO=getAutoLoadTO(pjp, arguments, cache, cacheKey, cacheWrapper);
                if(null != autoLoadTO) {// 只有当autoLoadTO时才是实际用户请求，不为null时，是AutoLoadHandler 发过来的请求
                    autoLoadTO.setLastRequestTime(startTime);
                }
            }
            if(null != autoLoadTO) {
                autoLoadTO.setLastLoadTime(startTime);
                autoLoadTO.addUseTotalTime(useTime);
            }
            return cacheWrapper;
        } catch(Throwable e) {
            throw e;
        } finally {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(false);
            }
        }
    }

    /**
     * 写缓存
     * @param pjp CacheAopProxyChain
     * @param autoLoadTO AutoLoadTO
     * @param cache Cache annotation
     * @param cacheKey Cache Key
     * @param cacheWrapper CacheWrapper
     * @return CacheWrapper
     * @throws Exception
     */
    private CacheWrapper writeCache(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, Cache cache, CacheKeyTO cacheKey,
        CacheWrapper cacheWrapper) throws Exception {
        if(null == cacheKey) {
            return null;
        }
        this.setCache(cacheKey, cacheWrapper);

        ExCache[] exCaches=cache.exCache();
        if(null != exCaches && exCaches.length > 0) {
            Object[] arguments=pjp.getArgs();
            if(null != autoLoadTO) {
                arguments=autoLoadTO.getArgs();
            }
            Object result=cacheWrapper.getCacheObject();
            for(ExCache exCache: exCaches) {
                if(!scriptParserUtil.isCacheable(exCache, arguments, result)) {
                    continue;
                }
                CacheKeyTO exCacheKey=getCacheKey(pjp, autoLoadTO, exCache, result);
                if(null == exCacheKey) {
                    continue;
                }
                Object exResult=null;
                if(null == exCache.cacheObject() || exCache.cacheObject().length() == 0) {
                    exResult=result;
                } else {
                    exResult=scriptParser.getElValue(exCache.cacheObject(), arguments, result, true, Object.class);
                }

                int exCacheExpire=scriptParserUtil.getRealExpire(exCache.expire(), exCache.expireExpression(), arguments, exResult);
                CacheWrapper exCacheWrapper=new CacheWrapper(exResult, exCacheExpire);
                AutoLoadTO tmpAutoLoadTO=this.autoLoadHandler.getAutoLoadTO(exCacheKey);
                if(null != tmpAutoLoadTO) {
                    tmpAutoLoadTO.setExpire(exCacheExpire);
                    tmpAutoLoadTO.setLastLoadTime(exCacheWrapper.getLastLoadTime());
                }
                this.setCache(exCacheKey, exCacheWrapper);
            }
        }
        return cacheWrapper;
    }

    @Override
    public void destroy() {
        autoLoadHandler.shutdown();
        autoLoadHandler=null;
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
     * @throws Exception
     */
    private CacheKeyTO getCacheKey(String className, String methodName, Object[] arguments, String _key, String _hfield,
        Object result, boolean hasRetVal) throws Exception {
        String key=null;
        String hfield=null;
        if(null != _key && _key.trim().length() > 0) {
            key=scriptParserUtil.getDefinedCacheKey(_key, arguments, result, hasRetVal);
            if(null != _hfield && _hfield.trim().length() > 0) {
                hfield=scriptParserUtil.getDefinedCacheKey(_hfield, arguments, result, hasRetVal);
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
     * @throws Exception
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache) throws Exception {
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
     * @throws Exception
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache, Object result) throws Exception {
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
     * @param cache
     * @param result 执行结果值
     * @return 缓存Key
     * @throws Exception
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, AutoLoadTO autoLoadTO, ExCache cache, Object result) throws Exception {
        String className=pjp.getTargetClass().getName();
        String methodName=pjp.getMethod().getName();
        Object[] arguments=pjp.getArgs();
        if(null != autoLoadTO) {
            arguments=autoLoadTO.getArgs();
        }
        String _key=cache.key();
        if(null == _key || _key.trim().length() == 0) {
            return null;
        }
        String _hfield=cache.hfield();
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
    private CacheKeyTO getCacheKey(DeleteCacheAopProxyChain jp, CacheDeleteKey cacheDeleteKey, Object retVal) throws Exception {
        String className=jp.getTargetClass().getName();
        String methodName=jp.getMethod().getName();
        Object[] arguments=jp.getArgs();
        String _key=cacheDeleteKey.value();
        String _hfield=cacheDeleteKey.hfield();
        return getCacheKey(className, methodName, arguments, _key, _hfield, retVal, true);

    }

    /**
     * 获取 AutoLoadTO
     * @param pjp
     * @param arguments
     * @param cache
     * @param cacheKey
     * @param cacheWrapper
     * @return
     * @throws Exception
     */
    private AutoLoadTO getAutoLoadTO(CacheAopProxyChain pjp, Object[] arguments, Cache cache, CacheKeyTO cacheKey,
        CacheWrapper cacheWrapper) throws Exception {
        AutoLoadTO autoLoadTO=null;
        if(scriptParserUtil.isAutoload(cache, arguments, cacheWrapper.getCacheObject())) {
            autoLoadTO=autoLoadHandler.getAutoLoadTO(cacheKey);
            if(null == autoLoadTO) {
                AutoLoadTO tmp=autoLoadHandler.putIfAbsent(cacheKey, pjp, cache, serializer, cacheWrapper);
                if(null != tmp) {
                    autoLoadTO=tmp;
                }
            }
        }
        return autoLoadTO;
    }
}
