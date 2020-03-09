package com.jarvis.cache;

import com.jarvis.cache.annotation.CacheDeleteMagicKey;
import com.jarvis.cache.aop.DeleteCacheAopProxyChain;
import com.jarvis.cache.to.CacheKeyTO;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Magic模式
 *
 *
 */
public class DeleteCacheMagicHandler {
    private final CacheHandler cacheHandler;

    private final DeleteCacheAopProxyChain jp;

    private final CacheDeleteMagicKey[] cacheDeleteKeys;

    private final Object[] arguments;

    private final Method method;

    private final Object retVal;

    private final Object target;

    private final String methodName;

    private final Class<?>[] parameterTypes;

    public DeleteCacheMagicHandler(CacheHandler cacheHandler, DeleteCacheAopProxyChain jp, CacheDeleteMagicKey[] cacheDeleteKeys, Object retVal) {
        this.cacheHandler = cacheHandler;
        this.jp = jp;
        this.cacheDeleteKeys = cacheDeleteKeys;
        this.arguments = jp.getArgs();
        this.method = jp.getMethod();
        this.retVal = retVal;
        this.target = jp.getTarget();
        this.methodName = jp.getMethod().getName();
        this.parameterTypes = method.getParameterTypes();
    }

    /**
     * @param cacheDeleteKey
     * @return
     * @throws Exception
     */
    private void isMagic(CacheDeleteMagicKey cacheDeleteKey) throws Exception {
        String key = cacheDeleteKey.value();
        if (null == key || key.length() == 0) {
            throw new Exception("value不允许为空");
        }
        int iterableArgIndex = cacheDeleteKey.iterableArgIndex();
        if (parameterTypes.length > 0 && iterableArgIndex >= 0) {
            if (iterableArgIndex >= parameterTypes.length) {
                throw new Exception("iterableArgIndex必须小于参数长度：" + parameterTypes.length);
            }
            if (iterableArgIndex >= 0 && cacheDeleteKey.iterableReturnValue()) {
                throw new Exception("不支持iterableArgIndex大于0且iterableReturnValue=true的情况");
            }
            Class<?> tmp = parameterTypes[iterableArgIndex];
            if (tmp.isArray() || Collection.class.isAssignableFrom(tmp)) {
                //rv = true;
            } else {
                throw new Exception("magic模式下，参数" + iterableArgIndex + "必须是数组或Collection的类型");
            }
        }
        if (cacheDeleteKey.iterableReturnValue()) {
            Class<?> returnType = method.getReturnType();
            if (returnType.isArray() || Collection.class.isAssignableFrom(returnType)) {
                // rv = true;
            } else {
                throw new Exception("当iterableReturnValue=true时，返回值必须是数组或Collection的类型");
            }
        }
    }

    public List<List<CacheKeyTO>> getCacheKeyForMagic() throws Exception {
        List<List<CacheKeyTO>> lists = new ArrayList<>(cacheDeleteKeys.length);
        for (CacheDeleteMagicKey cacheDeleteKey : cacheDeleteKeys) {
            isMagic(cacheDeleteKey);
            String keyExpression = cacheDeleteKey.value();
            String hfieldExpression = cacheDeleteKey.hfield();
            // 只对返回值进行分割处理
            if (parameterTypes.length == 0 || cacheDeleteKey.iterableArgIndex() < 0) {
                if (cacheDeleteKey.iterableReturnValue()) {
                    lists.add(splitReturnValueOnly(cacheDeleteKey, retVal, keyExpression, hfieldExpression));
                }
                continue;
            }
            int iterableArgIndex = cacheDeleteKey.iterableArgIndex();
            // 只对参数进行分割处理
            if (iterableArgIndex >= 0 && !cacheDeleteKey.iterableReturnValue()) {
                lists.add(splitArgOnly(cacheDeleteKey, retVal, keyExpression, hfieldExpression));
                continue;
            }
            if (iterableArgIndex >= 0 && cacheDeleteKey.iterableReturnValue()) {

            }

        }
        return lists;
    }

    private List<CacheKeyTO> splitReturnValueOnly(CacheDeleteMagicKey cacheDeleteKey, Object retVal, String keyExpression, String hfieldExpression) throws Exception {
        if (null == retVal) {
            return Collections.emptyList();
        }
        List<CacheKeyTO> list;
        if (retVal.getClass().isArray()) {
            Object[] newValues = (Object[]) retVal;
            list = new ArrayList<>(newValues.length);
            for (Object value : newValues) {
                if (!cacheHandler.getScriptParser().isCanDelete(cacheDeleteKey, arguments, value)) {
                    continue;
                }
                list.add(this.cacheHandler.getCacheKey(target, methodName, arguments, keyExpression, hfieldExpression, value, true));
            }
        } else if (retVal instanceof Collection) {
            Collection<Object> newValues = (Collection<Object>) retVal;
            list = new ArrayList<>(newValues.size());
            for (Object value : newValues) {
                if (!cacheHandler.getScriptParser().isCanDelete(cacheDeleteKey, arguments, value)) {
                    continue;
                }
                list.add(this.cacheHandler.getCacheKey(target, methodName, arguments, keyExpression, hfieldExpression, value, true));
            }
        } else {
            return Collections.emptyList();
        }
        return list;
    }

    private List<CacheKeyTO> splitArgOnly(CacheDeleteMagicKey cacheDeleteKey, Object retVal, String keyExpression, String hfieldExpression) throws Exception {
        int iterableArgIndex = cacheDeleteKey.iterableArgIndex();
        Object tmpArg = arguments[iterableArgIndex];
        List<CacheKeyTO> list = null;
        if (tmpArg instanceof Collection) {
            Collection<Object> iterableCollectionArg = (Collection<Object>) tmpArg;
            list = new ArrayList<>(iterableCollectionArg.size());
            for (Object arg : iterableCollectionArg) {
                Optional<CacheKeyTO> tmp = getKey(arg, cacheDeleteKey, retVal, keyExpression, hfieldExpression);
                if (tmp.isPresent()) {
                    list.add(tmp.get());
                }
            }
        } else if (tmpArg.getClass().isArray()) {
            Object[] iterableArrayArg = (Object[]) tmpArg;
            list = new ArrayList<>(iterableArrayArg.length);
            for (Object arg : iterableArrayArg) {
                Optional<CacheKeyTO> tmp = getKey(arg, cacheDeleteKey, retVal, keyExpression, hfieldExpression);
                if (tmp.isPresent()) {
                    list.add(tmp.get());
                }
            }
        } else {
            return Collections.emptyList();
        }
        return list;
    }

    private Optional<CacheKeyTO> getKey(Object arg, CacheDeleteMagicKey cacheDeleteKey, Object retVal, String keyExpression, String hfieldExpression) throws Exception {
        Object[] tmpArgs = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (i == cacheDeleteKey.iterableArgIndex()) {
                tmpArgs[i] = arg;
            } else {
                tmpArgs[i] = arguments[i];
            }
        }
        if (!cacheHandler.getScriptParser().isCanDelete(cacheDeleteKey, tmpArgs, retVal)) {
            return Optional.empty();
        }
        return Optional.of(this.cacheHandler.getCacheKey(target, methodName, tmpArgs, keyExpression, hfieldExpression, retVal, true));
    }
}
