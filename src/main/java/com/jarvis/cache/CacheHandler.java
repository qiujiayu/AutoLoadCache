package com.jarvis.cache;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.annotation.CacheDeleteTransactional;
import com.jarvis.cache.annotation.ExCache;
import com.jarvis.cache.annotation.Magic;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.aop.DeleteCacheAopProxyChain;
import com.jarvis.cache.aop.DeleteCacheTransactionalAopProxyChain;
import com.jarvis.cache.clone.ICloner;
import com.jarvis.cache.exception.CacheCenterConnectionException;
import com.jarvis.cache.lock.ILock;
import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.to.AutoLoadConfig;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.cache.to.ProcessingTO;
import com.jarvis.cache.type.CacheOpType;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理AOP
 *
 * @author jiayu.qiu
 */
@Slf4j
public class CacheHandler {

    /**
     * 正在处理中的请求
     */
    public final ConcurrentHashMap<CacheKeyTO, ProcessingTO> processing;

    private final ICacheManager cacheManager;

    private final AutoLoadConfig config;

    private final ICloner cloner;

    private final AutoLoadHandler autoLoadHandler;

    /**
     * 表达式解析器
     */
    private final AbstractScriptParser scriptParser;

    private final RefreshHandler refreshHandler;

    /**
     * 分布式锁
     */
    private ILock lock;

    private ChangeListener changeListener;

    public CacheHandler(ICacheManager cacheManager, AbstractScriptParser scriptParser, AutoLoadConfig config,
                        ICloner cloner) {
        this.processing = new ConcurrentHashMap<>(config.getProcessingMapSize());
        this.cacheManager = cacheManager;
        this.config = config;
        this.cloner = cloner;
        this.autoLoadHandler = new AutoLoadHandler(this, config);
        this.scriptParser = scriptParser;
        registerFunction(config.getFunctions());
        refreshHandler = new RefreshHandler(this, config);
    }

    /**
     * 从数据源中获取最新数据，并写入缓存。注意：这里不使用“拿来主义”机制，是因为当前可能是更新数据的方法。
     *
     * @param pjp   CacheAopProxyChain
     * @param cache Cache注解
     * @return 最新数据
     * @throws Throwable 异常
     */
    private Object writeOnly(CacheAopProxyChain pjp, Cache cache) throws Throwable {
        DataLoaderFactory factory = DataLoaderFactory.getInstance();
        DataLoader dataLoader = factory.getDataLoader();
        CacheWrapper<Object> cacheWrapper;
        try {
            cacheWrapper = dataLoader.init(pjp, cache, this).getData().getCacheWrapper();
        } catch (Throwable e) {
            throw e;
        } finally {
            factory.returnObject(dataLoader);
        }
        Object result = cacheWrapper.getCacheObject();
        Object[] arguments = pjp.getArgs();
        if (scriptParser.isCacheable(cache, pjp.getTarget(), arguments, result)) {
            CacheKeyTO cacheKey = getCacheKey(pjp, cache, result);
            // 注意：这里只能获取AutoloadTO，不能生成AutoloadTO
            AutoLoadTO autoLoadTO = autoLoadHandler.getAutoLoadTO(cacheKey);
            try {
                writeCache(pjp, pjp.getArgs(), cache, cacheKey, cacheWrapper);
                if (null != autoLoadTO) {
                    // 同步加载时间
                    autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime())
                            // 同步过期时间
                            .setExpire(cacheWrapper.getExpire());
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return result;
    }

    /**
     * 获取CacheOpType，从三个地方获取：<br>
     * 1. Cache注解中获取；<br>
     * 2. 从ThreadLocal中获取；<br>
     * 3. 从参数中获取；<br>
     * 上面三者的优先级：从低到高。
     *
     * @param cache     注解
     * @param arguments 参数
     * @return CacheOpType
     */
    private CacheOpType getCacheOpType(Cache cache, Object[] arguments) {
        CacheOpType opType = cache.opType();
        CacheOpType tmpOpType = CacheHelper.getCacheOpType();
        if (null != tmpOpType) {
            opType = tmpOpType;
        }
        if (null != arguments && arguments.length > 0) {
            for (Object tmp : arguments) {
                if (null != tmp && tmp instanceof CacheOpType) {
                    opType = (CacheOpType) tmp;
                    break;
                }
            }
        }
        if (null == opType) {
            opType = CacheOpType.READ_WRITE;
        }
        return opType;
    }

    private boolean isMagic(Cache cache, Method method) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 参数支持一个 List\Set\数组\可变长参数
        boolean rv = null != parameterTypes && parameterTypes.length == 1 && cache.magic().key().length() > 0;
        if (rv) {
            Class<?> tmp = parameterTypes[0];
            if (tmp.isArray() || Collection.class.isAssignableFrom(tmp)) {
                //rv = true;
            } else {
                throw new Exception("magic模式下，参数必须是数组或Collection的类型");
            }
            Class<?> returnType = method.getReturnType();
            if (returnType.isArray() || Collection.class.isAssignableFrom(returnType)) {
                // rv = true;
            } else {
                throw new Exception("magic模式下，返回值必须是数组或Collection的类型");
            }
        }
        return rv;
    }

    private Object magic(CacheAopProxyChain pjp, Cache cache) throws Throwable {
        Map<CacheKeyTO, Object> keyArgMap = getCacheKeyForMagic(pjp, cache);
        Method method = pjp.getMethod();
        Map<CacheKeyTO, CacheWrapper<Object>> cacheValues = this.cacheManager.mget(method, keyArgMap.keySet());
        Class<?> returnType = method.getReturnType();
        // 如果所有key都已经命中
        if (keyArgMap.size() == cacheValues.size()) {
            return convertToReturnObject(cacheValues, returnType, null);
        }
        Iterator<Map.Entry<CacheKeyTO, Object>> keyArgMapIt = keyArgMap.entrySet().iterator();

        Object[] args;
        int argSize = keyArgMap.size() - cacheValues.size();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Map<CacheKeyTO, MSetParam> unmatchCache = new HashMap<>(argSize);
        if (parameterTypes.length == 1 && Collection.class.isAssignableFrom(parameterTypes[0])) {
            List<Object> argList = new ArrayList<>(argSize);
            while (keyArgMapIt.hasNext()) {
                Map.Entry<CacheKeyTO, Object> item = keyArgMapIt.next();
                if (!cacheValues.containsKey(item.getKey())) {
                    argList.add(item.getValue());
                    unmatchCache.put(item.getKey(), new MSetParam(item.getKey(), null));
                }
            }
            args = new Object[]{argList};
        } else {
            Object[] arguments = pjp.getArgs();
            Object arg = ((Object[]) arguments[0])[0];
            // 可变及数组参数
            Object[] args2 = (Object[]) Array.newInstance(arg.getClass(), argSize);
            int i = 0;
            while (keyArgMapIt.hasNext()) {
                Map.Entry<CacheKeyTO, Object> item = keyArgMapIt.next();
                if (!cacheValues.containsKey(item.getKey())) {
                    args2[i] = item.getValue();
                    unmatchCache.put(item.getKey(), new MSetParam(item.getKey(), null));
                    i++;
                }
            }
            args = new Object[]{args2};
        }
        Object newValue = this.getData(pjp, args);
        writeMagicCache(pjp, cache, newValue, unmatchCache);
        return convertToReturnObject(cacheValues, returnType, newValue);
    }

    private void writeMagicCache(CacheAopProxyChain pjp, Cache cache, Object newValue, Map<CacheKeyTO, MSetParam> unmatchCache) throws Exception {
        if (null != newValue) {
            Magic magic = cache.magic();
            Object target = pjp.getTarget();
            String methodName = pjp.getMethod().getName();
            String keyExpression = magic.key();
            String hfieldExpression = magic.hfield();
            if (newValue.getClass().isArray()) {
                Object[] newValues = (Object[]) newValue;
                for (Object value : newValues) {
                    genCacheWrapper(target, methodName, keyExpression, hfieldExpression, value, unmatchCache);
                }
            } else if (newValue instanceof Collection) {
                Collection<Object> newValues = (Collection<Object>) newValue;
                for (Object value : newValues) {
                    genCacheWrapper(target, methodName, keyExpression, hfieldExpression, value, unmatchCache);
                }
            } else {
                throw new Exception("Magic模式返回值，只允许是数组或Collection类型的");
            }
        }
        Iterator<Map.Entry<CacheKeyTO, MSetParam>> unmatchCacheIt = unmatchCache.entrySet().iterator();
        while (unmatchCacheIt.hasNext()) {
            Map.Entry<CacheKeyTO, MSetParam> entry = unmatchCacheIt.next();
            MSetParam param = entry.getValue();
            CacheWrapper<Object> cacheWrapper = param.getResult();
            if (cacheWrapper == null) {
                cacheWrapper = new CacheWrapper<>();
                param.setResult(cacheWrapper);
            }
            int expire = getScriptParser().getRealExpire(cache.expire(), cache.expireExpression(), null, cacheWrapper.getCacheObject());
            cacheWrapper.setExpire(expire);
            if (expire < 0) {
                unmatchCacheIt.remove();
            }
            if (log.isDebugEnabled()) {
                String isNull = null == cacheWrapper.getCacheObject() ? "is null" : "is not null";
                log.debug("the data for key :" + entry.getKey() + " is from datasource " + isNull + ", expire :" + expire);
            }
        }
        this.cacheManager.mset(pjp.getMethod(), unmatchCache.values());
    }

    private void genCacheWrapper(Object target, String methodName, String keyExpression, String hfieldExpression, Object value, Map<CacheKeyTO, MSetParam> unmatchCache) throws Exception {
        CacheKeyTO cacheKeyTO = getCacheKey(target, methodName, null, keyExpression, hfieldExpression, value, true);
        MSetParam param = unmatchCache.get(cacheKeyTO);
        if (param == null) {
            // 通过 magic生成的CacheKeyTO 与通过参数生成的CacheKeyTO 匹配不上
            throw new Exception("通过 magic生成的CacheKeyTO 与通过参数生成的CacheKeyTO 匹配不上");
        }
        CacheWrapper<Object> cacheWrapper = new CacheWrapper<>();
        cacheWrapper.setCacheObject(value);
        param.setResult(cacheWrapper);
    }

    private Object convertToReturnObject(Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, Class<?> returnType, Object newValue) throws Throwable {
        if (returnType.isArray()) {
            int newValueSize = 0;
            Object[] newValues = (Object[]) newValue;
            if (null != newValues) {
                newValueSize = newValues.length;
            }
            Object[] res = new Object[cacheValues.size() + newValueSize];
            Iterator<Map.Entry<CacheKeyTO, CacheWrapper<Object>>> cacheValuesIt = cacheValues.entrySet().iterator();
            int i = 0;
            while (cacheValuesIt.hasNext()) {
                Map.Entry<CacheKeyTO, CacheWrapper<Object>> item = cacheValuesIt.next();
                if (log.isDebugEnabled()) {
                    log.debug("the data for key:" + item.getKey() + " is from cache, expire :" + item.getValue().getExpire());
                }
                Object data = item.getValue().getCacheObject();
                if (null != data) {
                    res[i] = data;
                    i++;
                }
            }
            if (null != newValues) {
                for (Object value : newValues) {
                    if (null != value) {
                        res[i] = value;
                        i++;
                    }
                }
            }
            return res;
        } else {
            Collection<Object> res;
            int newValueSize = 0;
            Collection<Object> newValues = (Collection<Object>) newValue;
            if (null != newValues) {
                newValueSize = newValues.size();
            }

            if (LinkedList.class.isAssignableFrom(returnType)) {
                res = new LinkedList<>();
            } else if (List.class.isAssignableFrom(returnType)) {
                res = new ArrayList<>(cacheValues.size() + newValueSize);
            } else if (LinkedHashSet.class.isAssignableFrom(returnType)) {
                res = new LinkedHashSet<>(cacheValues.size() + newValueSize);
            } else if (Set.class.isAssignableFrom(returnType)) {
                res = new HashSet<>(cacheValues.size() + newValueSize);
            } else {
                throw new Exception("Unsupported return type:" + returnType.getName());
            }
            cacheToCollection(cacheValues, res);
            if (null != newValues) {
                for (Object value : newValues) {
                    if (null != value) {
                        res.add(value);
                    }
                }
            }
            return res;
        }
    }

    private void cacheToCollection(Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, Collection<Object> out) {
        Iterator<Map.Entry<CacheKeyTO, CacheWrapper<Object>>> cacheValuesIt = cacheValues.entrySet().iterator();
        while (cacheValuesIt.hasNext()) {
            Map.Entry<CacheKeyTO, CacheWrapper<Object>> item = cacheValuesIt.next();
            if (log.isDebugEnabled()) {
                log.debug("the data for key:" + item.getKey() + " is from cache, expire :" + item.getValue().getExpire());
            }
            Object data = item.getValue().getCacheObject();
            if (null != data) {
                out.add(data);
            }
        }
    }


    /**
     * 处理@Cache 拦截
     *
     * @param pjp   切面
     * @param cache 注解
     * @return T 返回值
     * @throws Exception 异常
     */
    public Object proceed(CacheAopProxyChain pjp, Cache cache) throws Throwable {
        Object[] arguments = pjp.getArgs();
        CacheOpType opType = getCacheOpType(cache, arguments);
        if (log.isTraceEnabled()) {
            log.trace("CacheHandler.proceed-->{}.{}--{})", pjp.getTarget().getClass().getName(), pjp.getMethod().getName(), opType.name());
        }
        if (opType == CacheOpType.WRITE) {
            return writeOnly(pjp, cache);
        } else if (opType == CacheOpType.LOAD || !scriptParser.isCacheable(cache, pjp.getTarget(), arguments)) {
            return getData(pjp);
        }
        Method method = pjp.getMethod();
        if (isMagic(cache, method)) {
            return magic(pjp, cache);
        }

        CacheKeyTO cacheKey = getCacheKey(pjp, cache);
        if (null == cacheKey) {
            return getData(pjp);
        }
        CacheWrapper<Object> cacheWrapper = null;
        try {
            // 从缓存中获取数据
            cacheWrapper = this.get(cacheKey, method);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        if (log.isTraceEnabled()) {
            log.trace("cache key:{}, cache data is {} ", cacheKey.getCacheKey(), cacheWrapper);
        }
        if (opType == CacheOpType.READ_ONLY) {
            return null == cacheWrapper ? null : cacheWrapper.getCacheObject();
        }

        if (null != cacheWrapper && !cacheWrapper.isExpired()) {
            AutoLoadTO autoLoadTO = autoLoadHandler.putIfAbsent(cacheKey, pjp, cache, cacheWrapper);
            if (null != autoLoadTO) {
                autoLoadTO.flushRequestTime(cacheWrapper);
            } else {
                // 如果缓存快要失效，则自动刷新
                refreshHandler.doRefresh(pjp, cache, cacheKey, cacheWrapper);
            }
            return cacheWrapper.getCacheObject();
        }
        DataLoaderFactory factory = DataLoaderFactory.getInstance();
        DataLoader dataLoader = factory.getDataLoader();
        CacheWrapper<Object> newCacheWrapper = null;
        long loadDataUseTime = 0L;
        boolean isFirst;
        try {
            newCacheWrapper = dataLoader.init(pjp, cacheKey, cache, this).loadData().getCacheWrapper();
            isFirst = dataLoader.isFirst();
            loadDataUseTime = dataLoader.getLoadDataUseTime();
        } catch (Throwable e) {
            throw e;
        } finally {
            factory.returnObject(dataLoader);
        }
        if (isFirst) {
            AutoLoadTO autoLoadTO = autoLoadHandler.putIfAbsent(cacheKey, pjp, cache, newCacheWrapper);
            try {
                writeCache(pjp, pjp.getArgs(), cache, cacheKey, newCacheWrapper);
                if (null != autoLoadTO) {
                    autoLoadTO.flushRequestTime(cacheWrapper);
                    autoLoadTO.addUseTotalTime(loadDataUseTime);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        return newCacheWrapper.getCacheObject();
    }

    /**
     * 处理@CacheDelete 拦截
     *
     * @param jp          切点
     * @param cacheDelete 拦截到的注解
     * @param retVal      返回值
     * @throws Throwable 异常
     */
    public void deleteCache(DeleteCacheAopProxyChain jp, CacheDelete cacheDelete, Object retVal) throws Throwable {
        Object[] arguments = jp.getArgs();
        CacheDeleteKey[] keys = cacheDelete.value();
        if (null == keys || keys.length == 0) {
            return;
        }
        Object target = jp.getTarget();
        String methodName = jp.getMethod().getName();
        try {
            boolean isOnTransactional = CacheHelper.isOnTransactional();
            Set<CacheKeyTO> keySet = null;
            if (!isOnTransactional) {
                keySet = new HashSet<>(keys.length);
            }
            for (int i = 0; i < keys.length; i++) {
                CacheDeleteKey keyConfig = keys[i];
                String[] tempKeys = keyConfig.value();
                String tempHfield = keyConfig.hfield();
                if (!scriptParser.isCanDelete(keyConfig, arguments, retVal)) {
                    continue;
                }
                for (String tempKey : tempKeys) {
                    CacheKeyTO key = getCacheKey(target, methodName, arguments, tempKey, tempHfield, retVal, true);
                    if (null == key) {
                        continue;
                    }
                    if (isOnTransactional) {
                        CacheHelper.addDeleteCacheKey(key);
                    } else {
                        keySet.add(key);
                    }
                    this.getAutoLoadHandler().resetAutoLoadLastLoadTime(key);
                }
            }
            this.delete(keySet);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 用于处理事务下，事务处理完后才删除缓存，避免因事务失败造成缓存中的数据不一致问题。
     *
     * @param pjp                      切面
     * @param cacheDeleteTransactional 注解
     * @return Object 返回值
     * @throws Throwable 异常
     */
    public Object proceedDeleteCacheTransactional(DeleteCacheTransactionalAopProxyChain pjp,
                                                  CacheDeleteTransactional cacheDeleteTransactional) throws Throwable {
        Object result = null;
        Set<CacheKeyTO> set0 = CacheHelper.getDeleteCacheKeysSet();
        boolean isStart = null == set0;
        if (!cacheDeleteTransactional.useCache()) {
            // 在事务环境下尽量直接去数据源加载数据，而不是从缓存中加载，减少数据不一致的可能
            CacheHelper.setCacheOpType(CacheOpType.LOAD);
        }
        boolean getError = false;
        try {
            CacheHelper.initDeleteCacheKeysSet();// 初始化Set
            result = pjp.doProxyChain();
        } catch (Throwable e) {
            getError = true;
            throw e;
        } finally {
            CacheHelper.clearCacheOpType();
            if (isStart) {
                if (getError && !cacheDeleteTransactional.deleteCacheOnError()) {
                    // do nothing
                } else {
                    clearCache();
                }
            }
        }

        return result;
    }

    private void clearCache() throws Throwable {
        try {
            Set<CacheKeyTO> set = CacheHelper.getDeleteCacheKeysSet();
            if (null != set && set.size() > 0) {
                this.delete(set);
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("proceedDeleteCacheTransactional: key set is empty!");
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            // 抛出异常，让事务回滚，避免数据库和缓存双写不一致问题
            throw e;
        } finally {
            CacheHelper.clearDeleteCacheKeysSet();
        }
    }

    /**
     * 直接加载数据（加载后的数据不往缓存放）
     *
     * @param pjp CacheAopProxyChain
     * @return Object
     * @throws Throwable 异常
     */
    private Object getData(CacheAopProxyChain pjp) throws Throwable {
        return getData(pjp, pjp.getArgs());
    }

    private Object getData(CacheAopProxyChain pjp, Object[] arguments) throws Throwable {
        try {
            long startTime = System.currentTimeMillis();
            Object result = pjp.doProxyChain(arguments);
            long useTime = System.currentTimeMillis() - startTime;
            if (config.isPrintSlowLog() && useTime >= config.getSlowLoadTime()) {
                String className = pjp.getTarget().getClass().getName();
                log.error("{}.{}, use time:{}ms", className, pjp.getMethod().getName(), useTime);
            }
            return result;
        } catch (Throwable e) {
            throw e;
        }
    }

    public void writeCache(CacheAopProxyChain pjp, Object[] arguments, Cache cache, CacheKeyTO cacheKey,
                           CacheWrapper<Object> cacheWrapper) throws Exception {
        if (null == cacheKey) {
            return;
        }
        ExCache[] exCaches = cache.exCache();
        Method method = pjp.getMethod();
        List<MSetParam> params = new ArrayList<>(exCaches.length + 1);
        if (cacheWrapper.getExpire() >= 0) {
            params.add(new MSetParam(cacheKey, cacheWrapper));
        }

        Object result = cacheWrapper.getCacheObject();
        Object target = pjp.getTarget();
        for (ExCache exCache : exCaches) {
            try {
                if (!scriptParser.isCacheable(exCache, pjp.getTarget(), arguments, result)) {
                    continue;
                }
                CacheKeyTO exCacheKey = getCacheKey(pjp, arguments, exCache, result);
                if (null == exCacheKey) {
                    continue;
                }
                Object exResult = null;
                if (null == exCache.cacheObject() || exCache.cacheObject().isEmpty()) {
                    exResult = result;
                } else {
                    exResult = scriptParser.getElValue(exCache.cacheObject(), target, arguments, result, true,
                            Object.class);
                }

                int exCacheExpire = scriptParser.getRealExpire(exCache.expire(), exCache.expireExpression(), arguments,
                        exResult);
                CacheWrapper<Object> exCacheWrapper = new CacheWrapper<Object>(exResult, exCacheExpire);
                AutoLoadTO tmpAutoLoadTO = this.autoLoadHandler.getAutoLoadTO(exCacheKey);
                if (exCacheExpire >= 0) {
                    params.add(new MSetParam(exCacheKey, exCacheWrapper));
                    if (null != tmpAutoLoadTO) {
                        tmpAutoLoadTO.setExpire(exCacheExpire)
                                //
                                .setLastLoadTime(exCacheWrapper.getLastLoadTime());
                    }
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        int size = params.size();
        if (size == 1) {
            MSetParam param = params.get(0);
            this.setCache(param.getCacheKey(), param.getResult(), method);
        } else if (size > 1) {
            this.mset(method, params);
        }
    }

    public void destroy() {
        autoLoadHandler.shutdown();
        refreshHandler.shutdown();
        log.trace("cache destroy ... ... ...");
    }

    /**
     * 生成缓存KeyTO
     *
     * @param target           类名
     * @param methodName       方法名
     * @param arguments        参数
     * @param keyExpression    key表达式
     * @param hfieldExpression hfield表达式
     * @param result           执行实际方法的返回值
     * @return CacheKeyTO
     */
    private CacheKeyTO getCacheKey(Object target, String methodName, Object[] arguments, String keyExpression,
                                   String hfieldExpression, Object result, boolean hasRetVal) {
        String key = null;
        String hfield = null;
        if (null != keyExpression && keyExpression.trim().length() > 0) {
            try {
                key = scriptParser.getDefinedCacheKey(keyExpression, target, arguments, result, hasRetVal);
                if (null != hfieldExpression && hfieldExpression.trim().length() > 0) {
                    hfield = scriptParser.getDefinedCacheKey(hfieldExpression, target, arguments, result, hasRetVal);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        } else {
            key = CacheUtil.getDefaultCacheKey(target.getClass().getName(), methodName, arguments);
        }
        if (null == key || key.trim().isEmpty()) {
            throw new IllegalArgumentException("cache key for " + target.getClass().getName() + "." + methodName + " is empty");
        }
        return new CacheKeyTO(config.getNamespace(), key, hfield);
    }

    /**
     * 生成缓存 Key
     *
     * @param pjp
     * @param cache
     * @return String 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache) {
        Object target = pjp.getTarget();
        String methodName = pjp.getMethod().getName();
        Object[] arguments = pjp.getArgs();
        String keyExpression = cache.key();
        String hfieldExpression = cache.hfield();
        return getCacheKey(target, methodName, arguments, keyExpression, hfieldExpression, null, false);
    }

    private Map<CacheKeyTO, Object> getCacheKeyForMagic(CacheAopProxyChain pjp, Cache cache) {
        Object target = pjp.getTarget();
        String methodName = pjp.getMethod().getName();
        Object[] arguments = pjp.getArgs();
        String keyExpression = cache.key();
        String hfieldExpression = cache.hfield();
        Map<CacheKeyTO, Object> keyArgMap = null;
        if (arguments[0] instanceof Collection) {
            Collection<Object> args = (Collection<Object>) arguments[0];
            keyArgMap = new HashMap<>(args.size());
            Object[] tmpArg;
            for (Object arg : args) {
                tmpArg = new Object[]{arg};
                keyArgMap.put(getCacheKey(target, methodName, tmpArg, keyExpression, hfieldExpression, null, false), arg);
            }
        } else if (arguments[0].getClass().isArray()) {
            Object[] args = (Object[]) arguments[0];
            keyArgMap = new HashMap<>(args.length);
            Object[] tmpArg;
            for (Object arg : args) {
                tmpArg = new Object[]{arg};
                keyArgMap.put(getCacheKey(target, methodName, tmpArg, keyExpression, hfieldExpression, null, false), arg);
            }
        }
        if (null == keyArgMap || keyArgMap.isEmpty()) {
            throw new IllegalArgumentException("the 'keys' array is empty");
        }
        return keyArgMap;
    }

    /**
     * 生成缓存 Key
     *
     * @param pjp
     * @param cache
     * @param result 执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Cache cache, Object result) {
        Object target = pjp.getTarget();
        String methodName = pjp.getMethod().getName();
        Object[] arguments = pjp.getArgs();
        String keyExpression = cache.key();
        String hfieldExpression = cache.hfield();
        return getCacheKey(target, methodName, arguments, keyExpression, hfieldExpression, result, true);
    }

    /**
     * 生成缓存 Key
     *
     * @param pjp
     * @param arguments
     * @param exCache
     * @param result    执行结果值
     * @return 缓存Key
     */
    private CacheKeyTO getCacheKey(CacheAopProxyChain pjp, Object[] arguments, ExCache exCache, Object result) {
        Object target = pjp.getTarget();
        String methodName = pjp.getMethod().getName();
        String keyExpression = exCache.key();
        if (null == keyExpression || keyExpression.trim().length() == 0) {
            return null;
        }
        String hfieldExpression = exCache.hfield();
        return getCacheKey(target, methodName, arguments, keyExpression, hfieldExpression, result, true);
    }

    public AutoLoadHandler getAutoLoadHandler() {
        return this.autoLoadHandler;
    }

    public AbstractScriptParser getScriptParser() {
        return scriptParser;
    }

    private void registerFunction(Map<String, String> funcs) {
        if (null == scriptParser) {
            return;
        }
        if (null == funcs || funcs.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, String>> it = funcs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            try {
                String name = entry.getKey();
                Class<?> cls = Class.forName(entry.getValue());
                Method method = cls.getDeclaredMethod(name, new Class[]{Object.class});
                scriptParser.addFunction(name, method);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public ILock getLock() {
        return lock;
    }

    public void setLock(ILock lock) {
        this.lock = lock;
    }

    public void setCache(CacheKeyTO cacheKey, CacheWrapper<Object> result, Method method) throws CacheCenterConnectionException {
        cacheManager.setCache(cacheKey, result, method);
        if (null != changeListener) {
            changeListener.update(cacheKey, result);
        }
    }

    public void mset(final Method method, final Collection<MSetParam> params) throws CacheCenterConnectionException {
        cacheManager.mset(method, params);
    }

    public CacheWrapper<Object> get(CacheKeyTO key, Method method) throws CacheCenterConnectionException {
        return cacheManager.get(key, method);
    }

    public void delete(Set<CacheKeyTO> keys) throws CacheCenterConnectionException {
        cacheManager.delete(keys);
        if (null != changeListener) {
            changeListener.delete(keys);
        }
    }

    public ICloner getCloner() {
        return cloner;
    }

    public AutoLoadConfig getAutoLoadConfig() {
        return this.config;
    }

    public ChangeListener getChangeListener() {
        return changeListener;
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

}
