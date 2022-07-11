package com.jarvis.cache;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.Magic;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * MagicHandler
 *
 *
 */
@Slf4j
public class MagicHandler {

    private final CacheHandler cacheHandler;

    private final CacheAopProxyChain pjp;

    private final Cache cache;

    private final Magic magic;

    private final Object[] arguments;

    private final int iterableArgIndex;

    private final Object[] iterableArrayArg;

    private final Collection<Object> iterableCollectionArg;

    private final Method method;

    private final Class<?> returnType;

    private CacheKeyTO[] cacheKeys;

    private final Class<?>[] parameterTypes;

    private final Object target;
    private final String methodName;

    public MagicHandler(CacheHandler cacheHandler, CacheAopProxyChain pjp, Cache cache) {
        this.cacheHandler = cacheHandler;
        this.pjp = pjp;
        this.cache = cache;

        this.magic = cache.magic();
        this.arguments = pjp.getArgs();
        this.iterableArgIndex = magic.iterableArgIndex();
        if (iterableArgIndex >= 0 && iterableArgIndex < arguments.length) {
            Object tmpArg = arguments[iterableArgIndex];
            if (tmpArg instanceof Collection) {
                this.iterableCollectionArg = (Collection<Object>) tmpArg;
                this.iterableArrayArg = null;
            } else if (tmpArg.getClass().isArray()) {
                this.iterableArrayArg = (Object[]) tmpArg;
                this.iterableCollectionArg = null;
            } else {
                this.iterableArrayArg = null;
                this.iterableCollectionArg = null;
            }
        } else {
            this.iterableArrayArg = null;
            this.iterableCollectionArg = null;
        }
        this.method = pjp.getMethod();
        this.returnType = method.getReturnType();
        this.parameterTypes = method.getParameterTypes();
        this.target = pjp.getTarget();
        this.methodName = pjp.getMethod().getName();
    }

    public static boolean isMagic(Cache cache, Method method) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Magic magic = cache.magic();
        int iterableArgIndex = magic.iterableArgIndex();
        String key = magic.key();
        boolean rv = null != key && key.length() > 0;
        if (rv) {
            // 有参方法
            if (parameterTypes.length > 0) {
                if (iterableArgIndex < 0) {
                    throw new Exception("iterableArgIndex必须大于或等于0");
                }
                if (iterableArgIndex >= parameterTypes.length) {
                    throw new Exception("iterableArgIndex必须小于参数长度：" + parameterTypes.length);
                }
                Class<?> tmp = parameterTypes[iterableArgIndex];
                // 参数支持一个 List\Set\数组\可变长参数
                if (tmp.isArray() || Collection.class.isAssignableFrom(tmp)) {
                    //rv = true;
                } else {
                    throw new Exception("magic模式下，参数" + iterableArgIndex + "必须是数组或Collection的类型");
                }
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

    private Collection<Object> newCollection(Class<?> collectionType, int resSize) throws Exception {
        Collection<Object> res;
        if (LinkedList.class.isAssignableFrom(collectionType)) {
            if (resSize == 0) {
                return Collections.emptyList();
            }
            res = new LinkedList<>();
        } else if (List.class.isAssignableFrom(collectionType)) {
            if (resSize == 0) {
                return Collections.emptyList();
            }
            res = new ArrayList<>(resSize);
        } else if (LinkedHashSet.class.isAssignableFrom(collectionType)) {
            if (resSize == 0) {
                return Collections.emptySet();
            }
            res = new LinkedHashSet<>(resSize);
        } else if (Set.class.isAssignableFrom(collectionType)) {
            if (resSize == 0) {
                return Collections.emptySet();
            }
            res = new HashSet<>(resSize);
        } else {
            throw new Exception("Unsupported type:" + collectionType.getName());
        }
        return res;
    }

    private Type getRealReturnType() {
        if (returnType.isArray()) {
            return returnType.getComponentType();
        } else {
            return ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        }
    }

    public Object magic() throws Throwable {
        // 如果是无参函数，直接从数据源加载数据，并写入缓存
        if (parameterTypes.length == 0) {
            Object newValue = this.cacheHandler.getData(pjp, null);
            writeMagicCache(newValue);
            return newValue;
        }
        Map<CacheKeyTO, Object> keyArgMap = getCacheKeyForMagic();
        if (null == keyArgMap || keyArgMap.isEmpty()) {
            if (returnType.isArray()) {
                return Array.newInstance(returnType.getComponentType(), 0);
            }
            return newCollection(returnType, 0);
        }
        Type returnItemType = getRealReturnType();
        Map<CacheKeyTO, CacheWrapper<Object>> cacheValues = this.cacheHandler.mget(method, returnItemType, keyArgMap.keySet());
        // 如果所有key都已经命中
        int argSize = keyArgMap.size() - cacheValues.size();
        if (argSize <= 0) {
            return convertToReturnObject(cacheValues, null, Collections.emptyMap());
        }
        Object[] args = getUnmatchArg(keyArgMap, cacheValues, argSize);
        Object newValue = this.cacheHandler.getData(pjp, args);
        args[iterableArgIndex] = null;
        Map<CacheKeyTO, MSetParam> unmatchCache = writeMagicCache(newValue, args, cacheValues, keyArgMap);
        return convertToReturnObject(cacheValues, newValue, unmatchCache);
    }

    /**
     * 过滤已经命中缓存的参数，将剩余参数进行重新组装
     *
     * @param keyArgMap
     * @param cacheValues
     * @param argSize
     * @return
     * @throws Exception
     */
    private Object[] getUnmatchArg(Map<CacheKeyTO, Object> keyArgMap, Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, int argSize) throws Exception {
        Iterator<Map.Entry<CacheKeyTO, Object>> keyArgMapIt = keyArgMap.entrySet().iterator();
        Object unmatchArg;
        if (null != iterableCollectionArg) {
            Collection<Object> argList = newCollection(iterableCollectionArg.getClass(), argSize);
            while (keyArgMapIt.hasNext()) {
                Map.Entry<CacheKeyTO, Object> item = keyArgMapIt.next();
                if (!cacheValues.containsKey(item.getKey())) {
                    argList.add(item.getValue());
                }
            }
            unmatchArg = argList;
        } else {
            Object arg = iterableArrayArg[0];
            // 可变及数组参数
            Object[] args = (Object[]) Array.newInstance(arg.getClass(), argSize);
            int i = 0;
            while (keyArgMapIt.hasNext()) {
                Map.Entry<CacheKeyTO, Object> item = keyArgMapIt.next();
                if (!cacheValues.containsKey(item.getKey())) {
                    args[i] = item.getValue();
                    i++;
                }
            }
            unmatchArg = args;
        }
        Object[] args = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (i == iterableArgIndex) {
                args[i] = unmatchArg;
            } else {
                args[i] = arguments[i];
            }
        }
        return args;
    }

    /**
     * 没有参数的情况下，将所有从数据源加载的数据写入缓存
     *
     * @param newValue 从数据源加载的数据
     * @return
     * @throws Exception
     */
    private Map<CacheKeyTO, MSetParam> writeMagicCache(Object newValue) throws Exception {
        if (null == newValue) {
            return Collections.emptyMap();
        }
        Map<CacheKeyTO, MSetParam> unmatchCache;
        Object[] args = null;
        if (newValue.getClass().isArray()) {
            Object[] newValues = (Object[]) newValue;
            unmatchCache = new HashMap<>(newValues.length);
            for (Object value : newValues) {
                MSetParam mSetParam = genCacheWrapper(value, args);
                unmatchCache.put(mSetParam.getCacheKey(), mSetParam);
            }
        } else if (newValue instanceof Collection) {
            Collection<Object> newValues = (Collection<Object>) newValue;
            unmatchCache = new HashMap<>(newValues.size());
            for (Object value : newValues) {
                MSetParam mSetParam = genCacheWrapper(value, args);
                unmatchCache.put(mSetParam.getCacheKey(), mSetParam);
            }
        } else {
            throw new Exception("Magic模式返回值，只允许是数组或Collection类型的");
        }
        if (null != unmatchCache && unmatchCache.size() > 0) {
            this.cacheHandler.mset(pjp.getMethod(), unmatchCache.values());
        }
        return unmatchCache;
    }

    /**
     * 有参数的情况下，将所有从数据源加载的数据写入缓存
     *
     * @param newValue    从数据源加载的数据
     * @param args        参数
     * @param cacheValues 已经命中缓存数据
     * @param keyArgMap   缓存Key与参数的映射
     * @return 返回所有未命中缓存的数据
     * @throws Exception
     */
    private Map<CacheKeyTO, MSetParam> writeMagicCache(Object newValue, Object[] args, Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, Map<CacheKeyTO, Object> keyArgMap) throws Exception {
        int cachedSize = null == cacheValues ? 0 : cacheValues.size();
        int unmatchSize = keyArgMap.size() - cachedSize;
        Map<CacheKeyTO, MSetParam> unmatchCache = new HashMap<>(unmatchSize);
        if (null != newValue) {
            if (newValue.getClass().isArray()) {
                Object[] newValues = (Object[]) newValue;
                for (Object value : newValues) {
                    MSetParam mSetParam = genCacheWrapper(value, args);
                    unmatchCache.put(mSetParam.getCacheKey(), mSetParam);
                }
            } else if (newValue instanceof Collection) {
                Collection<Object> newValues = (Collection<Object>) newValue;
                for (Object value : newValues) {
                    MSetParam mSetParam = genCacheWrapper(value, args);
                    unmatchCache.put(mSetParam.getCacheKey(), mSetParam);
                }
            } else {
                throw new Exception("Magic模式返回值，只允许是数组或Collection类型的");
            }
        }
        if (unmatchCache.size() < unmatchSize) {
            Set<CacheKeyTO> cacheKeySet = keyArgMap.keySet();
            // 为了避免缓存穿透问题，将数据源和缓存中都不存数据的Key，设置为null
            for (CacheKeyTO cacheKeyTO : cacheKeySet) {
                if (unmatchCache.containsKey(cacheKeyTO)) {
                    continue;
                }
                if (null != cacheValues && cacheValues.containsKey(cacheKeyTO)) {
                    continue;
                }
                MSetParam mSetParam = genCacheWrapper(cacheKeyTO, null, args);
                unmatchCache.put(mSetParam.getCacheKey(), mSetParam);
            }
        }
        if (null != unmatchCache && unmatchCache.size() > 0) {
            this.cacheHandler.mset(pjp.getMethod(), unmatchCache.values());
        }
        return unmatchCache;
    }

    private MSetParam genCacheWrapper(Object value, Object[] args) throws Exception {
        String keyExpression = magic.key();
        String hfieldExpression = magic.hfield();
        CacheKeyTO cacheKeyTO = this.cacheHandler.getCacheKey(target, methodName, args, keyExpression, hfieldExpression, value, true);
        int expire = this.cacheHandler.getScriptParser().getRealExpire(cache.expire(), cache.expireExpression(), args, value);
        return new MSetParam(cacheKeyTO, new CacheWrapper<>(value, expire));
    }

    private MSetParam genCacheWrapper(CacheKeyTO cacheKeyTO, Object value, Object[] args) throws Exception {
        int expire = this.cacheHandler.getScriptParser().getRealExpire(cache.expire(), cache.expireExpression(), args, value);
        return new MSetParam(cacheKeyTO, new CacheWrapper<>(value, expire));
    }

    /**
     * 将缓存数据和数据源获取数据合并，并转换成函数真正需要的返回值
     *
     * @param cacheValues
     * @param newValue
     * @param unmatchCache
     * @return
     * @throws Throwable
     */
    private Object convertToReturnObject(Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, Object newValue, Map<CacheKeyTO, MSetParam> unmatchCache) throws Throwable {
        if (returnType.isArray()) {
            int resSize;
            if (magic.returnNullValue()) {
                resSize = cacheKeys.length;
            } else {
                Object[] newValues = (Object[]) newValue;
                resSize = cacheValues.size() + (null == newValues ? 0 : newValues.length);
            }
            Object res = Array.newInstance(returnType.getComponentType(), resSize);
            int ind = 0;
            for (CacheKeyTO cacheKeyTO : cacheKeys) {
                Object val = getValueFormCacheOrDatasource(cacheKeyTO, cacheValues, unmatchCache);
                if (!magic.returnNullValue() && null == val) {
                    continue;
                }
                Array.set(res, ind, val);
                ind++;
            }
            return res;
        } else {
            int resSize;
            if (magic.returnNullValue()) {
                resSize = cacheKeys.length;
            } else {
                Collection<Object> newValues = (Collection<Object>) newValue;
                resSize = cacheValues.size() + (null == newValues ? 0 : newValues.size());
            }
            Collection<Object> res = newCollection(returnType, resSize);
            for (CacheKeyTO cacheKeyTO : cacheKeys) {
                Object val = getValueFormCacheOrDatasource(cacheKeyTO, cacheValues, unmatchCache);
                if (!magic.returnNullValue() && null == val) {
                    continue;
                }
                res.add(val);
            }
            return res;
        }
    }

    private Object getValueFormCacheOrDatasource(CacheKeyTO cacheKeyTO, Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, Map<CacheKeyTO, MSetParam> unmatchCache) {
        boolean isCache = false;
        CacheWrapper<Object> cacheWrapper = cacheValues.get(cacheKeyTO);
        if (null == cacheWrapper) {
            MSetParam mSetParam = unmatchCache.get(cacheKeyTO);
            if (null != mSetParam) {
                cacheWrapper = mSetParam.getResult();
            }
        } else {
            isCache = true;
        }
        Object val = null;
        if (null != cacheWrapper) {
            val = cacheWrapper.getCacheObject();
        }
        if (log.isDebugEnabled()) {
            String from = isCache ? "cache" : "datasource";
            String message = "the data for key:" + cacheKeyTO + " is from " + from;
            if (null != val) {
                message += ", value is not null";
            } else {
                message += ", value is null";
            }
            if (null != cacheWrapper) {
                message += ", expire :" + cacheWrapper.getExpire();
            }
            log.debug(message);
        }
        return val;
    }

    /**
     * 生成缓存Key
     *
     * @return
     */
    private Map<CacheKeyTO, Object> getCacheKeyForMagic() {
        Map<CacheKeyTO, Object> keyArgMap = null;
        if (null != iterableCollectionArg) {
            cacheKeys = new CacheKeyTO[iterableCollectionArg.size()];
            keyArgMap = new HashMap<>(iterableCollectionArg.size());
            int ind = 0;
            for (Object arg : iterableCollectionArg) {
                CacheKeyTO cacheKeyTO = buildCacheKey(arg);
                keyArgMap.put(cacheKeyTO, arg);
                cacheKeys[ind] = cacheKeyTO;
                ind++;
            }
        } else if (null != iterableArrayArg) {
            cacheKeys = new CacheKeyTO[iterableArrayArg.length];
            keyArgMap = new HashMap<>(iterableArrayArg.length);
            for (int ind = 0; ind < iterableArrayArg.length; ind++) {
                Object arg = iterableArrayArg[ind];
                CacheKeyTO cacheKeyTO = buildCacheKey(arg);
                keyArgMap.put(cacheKeyTO, arg);
                cacheKeys[ind] = cacheKeyTO;
            }
        }
        return keyArgMap;
    }

    private CacheKeyTO buildCacheKey(Object arg) {
        Object[] tmpArgs = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (i == iterableArgIndex) {
                tmpArgs[i] = arg;
            } else {
                tmpArgs[i] = arguments[i];
            }
        }
        String keyExpression = cache.key();
        String hfieldExpression = cache.hfield();
        return this.cacheHandler.getCacheKey(target, methodName, tmpArgs, keyExpression, hfieldExpression, null, false);
    }
}
