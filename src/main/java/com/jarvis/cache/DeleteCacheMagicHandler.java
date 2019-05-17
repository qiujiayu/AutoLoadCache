package com.jarvis.cache;

import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.aop.DeleteCacheAopProxyChain;
import com.jarvis.cache.to.CacheKeyTO;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeleteCacheMagicHandler {
    private final CacheHandler cacheHandler;

    private final DeleteCacheAopProxyChain jp;

    private final CacheDeleteKey cacheDeleteKey;

    private final Object[] arguments;

    private final int iterableArgIndex;

    private final Object[] iterableArrayArg;

    private final Collection<Object> iterableCollectionArg;

    private final Method method;

    // private final Class<?> returnType;

    private final Object retVal;

    public DeleteCacheMagicHandler(CacheHandler cacheHandler, DeleteCacheAopProxyChain jp, CacheDeleteKey cacheDeleteKey, Object retVal) {
        this.cacheHandler = cacheHandler;
        this.jp = jp;
        this.cacheDeleteKey = cacheDeleteKey;

        this.arguments = jp.getArgs();
        this.iterableArgIndex = cacheDeleteKey.iterableArgIndex();
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
        this.method = jp.getMethod();
        // this.returnType = method.getReturnType();
        this.retVal = retVal;
    }


    public static boolean isMagic(CacheDeleteKey cacheDeleteKey, Method method) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 参数支持一个 List\Set\数组\可变长参数
        int iterableArgIndex = cacheDeleteKey.iterableArgIndex();
        String[] keys = cacheDeleteKey.value();
        boolean rv = null != parameterTypes && null != keys && keys.length > 0;
        for (String key : keys) {
            if (null == key || key.length() == 0) {
                throw new Exception("缓存key中不能有空字符串");
            }
        }
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
        }
        return rv;
    }

    public List<CacheKeyTO> getCacheKeyForMagic() {
        Object target = jp.getTarget();
        String methodName = jp.getMethod().getName();
        String[] keyExpressions = cacheDeleteKey.value();
        String hfieldExpression = cacheDeleteKey.hfield();
        List<CacheKeyTO> list = null;
        if (null != iterableCollectionArg) {
            list = new ArrayList<>(iterableCollectionArg.size());
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
                for (String keyExpression : keyExpressions) {
                    list.add(this.cacheHandler.getCacheKey(target, methodName, tmpArgs, keyExpression, hfieldExpression, retVal, true));
                }
            }
        } else if (null != iterableArrayArg) {
            list = new ArrayList<>(iterableArrayArg.length);
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
                for (String keyExpression : keyExpressions) {
                    list.add(this.cacheHandler.getCacheKey(target, methodName, tmpArgs, keyExpression, hfieldExpression, retVal, true));
                }
            }
        }
        if (null == list || list.isEmpty()) {
            throw new IllegalArgumentException("the 'list' is empty");
        }
        return list;
    }
}
