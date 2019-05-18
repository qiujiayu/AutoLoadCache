package com.jarvis.cache;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.Magic;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.cache.to.CacheWrapper;
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

/**
 * MagicHandler
 *
 * @author jiayu.qiu
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

    // private final Class<?>[] parameterTypes;

    public MagicHandler(CacheHandler cacheHandler, CacheAopProxyChain pjp, Cache cache) {
        this.cacheHandler = cacheHandler;
        this.pjp = pjp;
        this.cache = cache;

        this.magic = cache.magic();
        this.arguments = pjp.getArgs();
        this.iterableArgIndex = magic.iterableArgIndex();
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
        this.method = pjp.getMethod();
        this.returnType = method.getReturnType();
        // this.parameterTypes = method.getParameterTypes();
    }

    public static boolean isMagic(Cache cache, Method method) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 参数支持一个 List\Set\数组\可变长参数
        Magic magic = cache.magic();
        int iterableArgIndex = magic.iterableArgIndex();
        String key = magic.key();
        boolean rv = null != parameterTypes && null != key && key.length() > 0;
        if (rv) {
            if (iterableArgIndex < 0) {
                throw new Exception("iterableArgIndex必须大于或等于0");
            }
            if (iterableArgIndex >= parameterTypes.length) {
                throw new Exception("iterableArgIndex必须小于参数长度：" + parameterTypes.length);
            }
            Class<?> tmp = parameterTypes[iterableArgIndex];
            if (tmp.isArray() || Collection.class.isAssignableFrom(tmp)) {
                //rv = true;
            } else {
                throw new Exception("magic模式下，参数" + iterableArgIndex + "必须是数组或Collection的类型");
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
            res = new LinkedList<>();
        } else if (List.class.isAssignableFrom(collectionType)) {
            res = new ArrayList<>(resSize);
        } else if (LinkedHashSet.class.isAssignableFrom(collectionType)) {
            res = new LinkedHashSet<>(resSize);
        } else if (Set.class.isAssignableFrom(collectionType)) {
            res = new HashSet<>(resSize);
        } else {
            throw new Exception("Unsupported type:" + collectionType.getName());
        }
        return res;
    }

    public Object magic() throws Throwable {
        Map<CacheKeyTO, Object> keyArgMap = getCacheKeyForMagic();
        Map<CacheKeyTO, CacheWrapper<Object>> cacheValues = this.cacheHandler.mget(method, keyArgMap.keySet());
        // 为了解决使用JSON反序列化时，缓存数据类型不正确问题
        if (null != cacheValues && !cacheValues.isEmpty()) {
            Iterator<Map.Entry<CacheKeyTO, CacheWrapper<Object>>> iterable = cacheValues.entrySet().iterator();
            while (iterable.hasNext()) {
                Map.Entry<CacheKeyTO, CacheWrapper<Object>> item = iterable.next();
                CacheWrapper<Object> cacheWrapper = item.getValue();
                Object cacheObject = cacheWrapper.getCacheObject();
                if (null == cacheObject) {
                    continue;
                }
                if (returnType.isArray() && cacheObject.getClass().isArray()) {
                    Object[] objects = (Object[]) cacheObject;
                    cacheWrapper.setCacheObject(objects[0]);
                } else if (Collection.class.isAssignableFrom(returnType) && this.returnType.isAssignableFrom(cacheObject.getClass())) {
                    Collection collection = (Collection) cacheObject;
                    Iterator tmp = collection.iterator();
                    cacheWrapper.setCacheObject(tmp.next());
                }
            }
        }
        // 如果所有key都已经命中
        int argSize = keyArgMap.size() - cacheValues.size();
        if (argSize == 0) {
            return convertToReturnObject(cacheValues, null);
        }
        Iterator<Map.Entry<CacheKeyTO, Object>> keyArgMapIt = keyArgMap.entrySet().iterator();
        Map<CacheKeyTO, MSetParam> unmatchCache = new HashMap<>(argSize);
        Object unmatchArg;
        if (null != iterableCollectionArg) {
            Collection<Object> argList = newCollection(iterableCollectionArg.getClass(), argSize);
            while (keyArgMapIt.hasNext()) {
                Map.Entry<CacheKeyTO, Object> item = keyArgMapIt.next();
                if (!cacheValues.containsKey(item.getKey())) {
                    argList.add(item.getValue());
                    unmatchCache.put(item.getKey(), new MSetParam(item.getKey(), null));
                }
            }
            unmatchArg = argList;
        } else {
            Object arg = iterableArrayArg[0];
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
            unmatchArg = args2;
        }
        Object[] args = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (i == iterableArgIndex) {
                args[i] = unmatchArg;
            } else {
                args[i] = arguments[i];
            }
        }
        Object newValue = this.cacheHandler.getData(pjp, args);
        args[iterableArgIndex] = null;
        writeMagicCache(newValue, unmatchCache, args);
        return convertToReturnObject(cacheValues, newValue);
    }

    private void writeMagicCache(Object newValue, Map<CacheKeyTO, MSetParam> unmatchCache, Object[] args) throws Exception {
        if (null != newValue) {
            Object target = pjp.getTarget();
            String methodName = pjp.getMethod().getName();
            String keyExpression = magic.key();
            String hfieldExpression = magic.hfield();
            if (newValue.getClass().isArray()) {
                Object[] newValues = (Object[]) newValue;
                for (Object value : newValues) {
                    genCacheWrapper(target, methodName, keyExpression, hfieldExpression, value, unmatchCache, args);
                }
            } else if (newValue instanceof Collection) {
                Collection<Object> newValues = (Collection<Object>) newValue;
                for (Object value : newValues) {
                    genCacheWrapper(target, methodName, keyExpression, hfieldExpression, value, unmatchCache, args);
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
            int expire = this.cacheHandler.getScriptParser().getRealExpire(cache.expire(), cache.expireExpression(), args, cacheWrapper.getCacheObject());
            cacheWrapper.setExpire(expire);
            if (expire < 0) {
                unmatchCacheIt.remove();
            }
            if (log.isDebugEnabled()) {
                String isNull = null == cacheWrapper.getCacheObject() ? "is null" : "is not null";
                log.debug("the data for key :" + entry.getKey() + " is from datasource " + isNull + ", expire :" + expire);
            }
        }
        this.cacheHandler.mset(pjp.getMethod(), unmatchCache.values());
    }

    private void genCacheWrapper(Object target, String methodName, String keyExpression, String hfieldExpression, Object value, Map<CacheKeyTO, MSetParam> unmatchCache, Object[] args) throws Exception {
        CacheKeyTO cacheKeyTO = this.cacheHandler.getCacheKey(target, methodName, args, keyExpression, hfieldExpression, value, true);
        MSetParam param = unmatchCache.get(cacheKeyTO);
        if (param == null) {
            // 通过 magic生成的CacheKeyTO 与通过参数生成的CacheKeyTO 匹配不上
            throw new Exception("通过 magic生成的CacheKeyTO 与通过参数生成的CacheKeyTO 匹配不上");
        }
        CacheWrapper<Object> cacheWrapper = new CacheWrapper<>();
        cacheWrapper.setCacheObject(value);
        param.setResult(cacheWrapper);
    }

    private Object convertToReturnObject(Map<CacheKeyTO, CacheWrapper<Object>> cacheValues, Object newValue) throws Throwable {
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
            int newValueSize = 0;
            Collection<Object> newValues = (Collection<Object>) newValue;
            if (null != newValues) {
                newValueSize = newValues.size();
            }
            int resSize = cacheValues.size() + newValueSize;
            Collection<Object> res = newCollection(returnType, resSize);
            Iterator<Map.Entry<CacheKeyTO, CacheWrapper<Object>>> cacheValuesIt = cacheValues.entrySet().iterator();
            while (cacheValuesIt.hasNext()) {
                Map.Entry<CacheKeyTO, CacheWrapper<Object>> item = cacheValuesIt.next();
                if (log.isDebugEnabled()) {
                    log.debug("the data for key:" + item.getKey() + " is from cache, expire :" + item.getValue().getExpire());
                }
                Object data = item.getValue().getCacheObject();
                if (null != data) {
                    res.add(data);
                }
            }
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

    private Map<CacheKeyTO, Object> getCacheKeyForMagic() {
        Object target = pjp.getTarget();
        String methodName = pjp.getMethod().getName();
        String keyExpression = cache.key();
        String hfieldExpression = cache.hfield();
        Map<CacheKeyTO, Object> keyArgMap = null;
        if (null != iterableCollectionArg) {
            keyArgMap = new HashMap<>(iterableCollectionArg.size());
            Object[] tmpArgs;
            for (Object arg : iterableCollectionArg) {
                tmpArgs = new Object[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    if (i == iterableArgIndex) {
                        tmpArgs[i] = arg;
                    } else {
                        tmpArgs[i] = arguments[i];
                    }
                }
                keyArgMap.put(this.cacheHandler.getCacheKey(target, methodName, tmpArgs, keyExpression, hfieldExpression, null, false), arg);
            }
        } else if (null != iterableArrayArg) {
            keyArgMap = new HashMap<>(iterableArrayArg.length);
            Object[] tmpArgs;
            for (Object arg : iterableArrayArg) {
                tmpArgs = new Object[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    if (i == iterableArgIndex) {
                        tmpArgs[i] = arg;
                    } else {
                        tmpArgs[i] = arguments[i];
                    }
                }
                keyArgMap.put(this.cacheHandler.getCacheKey(target, methodName, tmpArgs, keyExpression, hfieldExpression, null, false), arg);
            }
        }
        if (null == keyArgMap || keyArgMap.isEmpty()) {
            throw new IllegalArgumentException("the 'keyArgMap' is empty");
        }
        return keyArgMap;
    }
}
