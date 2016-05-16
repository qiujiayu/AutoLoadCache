package com.jarvis.cache;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.annotation.ExCache;
import com.jarvis.cache.type.CacheOpType;
import com.jarvis.lib.util.BeanUtil;

/**
 * @author jiayu.qiu
 */
public class CacheUtil {

    private static final String SPLIT_STR="_";

    private static final String ARGS="args";

    private static final String RET_VAL="retVal";

    private static final String HASH="hash";

    private static final ExpressionParser parser=new SpelExpressionParser();

    private static final ConcurrentHashMap<String, Expression> expCache=new ConcurrentHashMap<String, Expression>(64);

    private static final ConcurrentHashMap<String, Method> funcs=new ConcurrentHashMap<String, Method>(64);

    private static Method hash=null;

    private static Method empty=null;
    static {
        try {
            hash=CacheUtil.class.getDeclaredMethod("getUniqueHashStr", new Class[]{Object.class});
            empty=CacheUtil.class.getDeclaredMethod("isEmpty", new Class[]{Object.class});
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        } catch(SecurityException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void addFunction(String name, Method method) {
        funcs.put(name, method);
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Object obj) {
        if(null == obj) {
            return true;
        }
        if(obj instanceof String) {
            return ((String)obj).length() == 0;
        }
        Class cl=obj.getClass();
        if(cl.isArray()) {
            int len=Array.getLength(obj);
            return len == 0;
        }
        if(obj instanceof Collection) {
            Collection tempCol=(Collection)obj;
            return tempCol.isEmpty();
        }
        if(obj instanceof Map) {
            Map tempMap=(Map)obj;
            return tempMap.isEmpty();
        }
        return false;
    }

    /**
     * 生成字符串的HashCode
     * @param buf
     * @return int hashCode
     */
    private static int getHashCode(String buf) {
        int hash=5381;
        int len=buf.length();

        while(len-- > 0) {
            hash=((hash << 5) + hash) + buf.charAt(len); /* hash * 33 + c */
        }
        return hash;
    }

    /**
     * 将Object 对象转换为唯一的Hash字符串
     * @param obj Object
     * @return Hash字符串
     */
    public static String getUniqueHashStr(Object obj) {
        return getMiscHashCode(BeanUtil.toString(obj));
    }

    /**
     * 通过混合Hash算法，将长字符串转为短字符串（字符串长度小于等于20时，不做处理）
     * @param str String
     * @return Hash字符串
     */
    public static String getMiscHashCode(String str) {
        if(null == str || str.length() == 0) {
            return "";
        }
        if(str.length() <= 20) {
            return str;
        }
        StringBuilder tmp=new StringBuilder();
        tmp.append(str.hashCode()).append(SPLIT_STR).append(getHashCode(str));

        int mid=str.length() / 2;
        String str1=str.substring(0, mid);
        String str2=str.substring(mid);
        tmp.append(SPLIT_STR).append(str1.hashCode());
        tmp.append(SPLIT_STR).append(str2.hashCode());

        return tmp.toString();
    }

    /**
     * 将Spring EL 表达式转换期望的值
     * @param keySpEL 生成缓存Key的Spring el表达式
     * @param arguments 参数
     * @param valueType 值类型
     * @return T Value 返回值
     * @param <T> 泛型
     */
    public static <T> T getElValue(String keySpEL, Object[] arguments, Class<T> valueType) {
        return getElValue(keySpEL, arguments, null, false, valueType);
    }

    /**
     * 将Spring EL 表达式转换期望的值
     * @param keySpEL 生成缓存Key的Spring el表达式
     * @param arguments 参数
     * @param retVal 结果值
     * @param hasRetVal retVal 参数
     * @param valueType 值类型
     * @return T value 返回值
     * @param <T> 泛型
     */
    public static <T> T getElValue(String keySpEL, Object[] arguments, Object retVal, boolean hasRetVal, Class<T> valueType) {
        StandardEvaluationContext context=new StandardEvaluationContext();

        context.registerFunction(HASH, hash);
        context.registerFunction("empty", empty);
        Iterator<Map.Entry<String, Method>> it=funcs.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Method> entry=it.next();
            context.registerFunction(entry.getKey(), entry.getValue());
        }
        context.setVariable(ARGS, arguments);
        if(hasRetVal) {
            context.setVariable(RET_VAL, retVal);
        }
        Expression expression=expCache.get(keySpEL);
        if(null == expression) {
            expression=parser.parseExpression(keySpEL);
            expCache.put(keySpEL, expression);
        }
        return expression.getValue(context, valueType);
    }

    /**
     * 根据请求参数和执行结果值，进行构造缓存Key
     * @param keySpEL 生成缓存Key的Spring el表达式
     * @param arguments 参数
     * @param retVal 结果值
     * @param hasRetVal 是否有retVal
     * @return CacheKey 缓存Key
     */
    public static String getDefinedCacheKey(String keySpEL, Object[] arguments, Object retVal, boolean hasRetVal) {
        if(keySpEL.indexOf("#") == -1 && keySpEL.indexOf("'") == -1) {
            return keySpEL;
        }
        return getElValue(keySpEL, arguments, retVal, hasRetVal, String.class);
    }

    /**
     * 生成缓存Key
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @return CacheKey 缓存Key
     */
    public static String getDefaultCacheKey(String className, String method, Object[] arguments) {
        StringBuilder sb=new StringBuilder();
        sb.append(getDefaultCacheKeyPrefix(className, method, arguments));
        if(null != arguments && arguments.length > 0) {
            sb.append(getUniqueHashStr(arguments));
        }
        return sb.toString();
    }

    /**
     * 生成缓存Key的前缀
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @return CacheKey 缓存Key
     */
    public static String getDefaultCacheKeyPrefix(String className, String method, Object[] arguments) {
        StringBuilder sb=new StringBuilder();
        sb.append(className);
        if(null != method && method.length() > 0) {
            sb.append(".").append(method);
        }
        return sb.toString();
    }

    /**
     * 是否可以缓存
     * @param cache Cache
     * @param arguments 参数
     * @return cacheAble 是否可以进行缓存
     */
    public static boolean isCacheable(Cache cache, Object[] arguments) {
        boolean rv=true;
        if(null != arguments && arguments.length > 0 && null != cache.condition() && cache.condition().length() > 0) {
            rv=getElValue(cache.condition(), arguments, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以缓存
     * @param cache Cache
     * @param arguments 参数
     * @param result 执行结果
     * @return cacheAble 是否可以进行缓存
     */
    public static boolean isCacheable(Cache cache, Object[] arguments, Object result) {
        boolean rv=true;
        if(null != cache.condition() && cache.condition().length() > 0) {
            rv=getElValue(cache.condition(), arguments, result, true, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以缓存
     * @param cache ExCache
     * @param arguments 参数
     * @param result 执行结果
     * @return cacheAble 是否可以进行缓存
     */
    public static boolean isCacheable(ExCache cache, Object[] arguments, Object result) {
        if(null == cache || cache.expire() < 0 || cache.key().length() == 0) {
            return false;
        }
        boolean rv=true;
        if(null != cache.condition() && cache.condition().length() > 0) {
            rv=getElValue(cache.condition(), arguments, result, true, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以自动加载
     * @param cache Cache 注解
     * @param arguments 参数
     * @param retVal return value
     * @return autoload 是否自动加载
     */
    public static boolean isAutoload(Cache cache, Object[] arguments, Object retVal) {
        if(cache.opType() == CacheOpType.WRITE) {
            return false;
        }
        boolean autoload=cache.autoload();
        if(null != arguments && arguments.length > 0 && null != cache.autoloadCondition() && cache.autoloadCondition().length() > 0) {
            autoload=getElValue(cache.autoloadCondition(), arguments, retVal, true, Boolean.class);
        }
        return autoload;
    }

    /**
     * 是否可以删除缓存
     * @param cacheDeleteKey CacheDeleteKey注解
     * @param arguments 参数
     * @param retVal 结果值
     * @return Can Delete
     */
    public static boolean isCanDelete(CacheDeleteKey cacheDeleteKey, Object[] arguments, Object retVal) {
        boolean rv=true;
        if(null != arguments && arguments.length > 0 && null != cacheDeleteKey.condition()
            && cacheDeleteKey.condition().length() > 0) {
            rv=getElValue(cacheDeleteKey.condition(), arguments, retVal, true, Boolean.class);
        }
        return rv;
    }

    /**
     * 获取真实的缓存时间值
     * @param expire 缓存时间
     * @param expireExpression 缓存时间表达式
     * @param arguments 方法参数
     * @param result 方法执行返回结果
     * @return real expire
     */
    public static int getRealExpire(int expire, String expireExpression, Object[] arguments, Object result) {
        Integer tmpExpire=null;
        if(null != expireExpression && expireExpression.length() > 0) {
            try {
                tmpExpire=getElValue(expireExpression, arguments, result, true, Integer.class);
                if(null != tmpExpire && tmpExpire.intValue() >= 0) {
                    // 返回缓存时间表达式计算的时间
                    return tmpExpire.intValue();
                }
            } catch(Exception ex) {

            }
        }
        return expire;
    }

}
