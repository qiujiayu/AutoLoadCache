package com.jarvis.cache;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.lib.util.BeanUtil;

/**
 * @author jiayu.qiu
 */
public class CacheUtil {

    private static final String SPLIT_STR="_";

    private static final String ARGS="args";

    private static final String RET_VAL="retVal";

    private static final ExpressionParser parser=new SpelExpressionParser();

    private static final Pattern pattern_hash=Pattern.compile("(\\+?)\\$hash\\((.[^)]*)\\)");

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
        return getElValue(keySpEL, arguments, null, valueType);
    }

    /**
     * 将Spring EL 表达式转换期望的值
     * @param keySpEL 生成缓存Key的Spring el表达式
     * @param arguments 参数
     * @param valueType 值类型
     * @param retVal 结果值
     * @return T value 返回值
     * @param <T> 泛型
     */
    public static <T> T getElValue(String keySpEL, Object[] arguments, Object retVal, Class<T> valueType) {
        Matcher m=pattern_hash.matcher(keySpEL);
        StringBuffer sb=new StringBuffer();
        while(m.find()) {
            m.appendReplacement(sb, "$1T(com.jarvis.cache.CacheUtil).getUniqueHashStr($2)");
        }
        m.appendTail(sb);
        EvaluationContext context=new StandardEvaluationContext();
        context.setVariable(ARGS, arguments);
        context.setVariable(RET_VAL, retVal);
        return parser.parseExpression(sb.toString()).getValue(context, valueType);
    }

    /**
     * 生成自定义缓存Key
     * @param keySpEL 生成缓存Key的Spring el表达式
     * @param arguments 参数
     * @return cacheKey 生成的缓存Key
     */
    public static String getDefinedCacheKey(String keySpEL, Object[] arguments) {
        if(keySpEL.indexOf("#" + ARGS) != -1) {
            return getElValue(keySpEL, arguments, String.class);
        } else {
            return keySpEL;
        }
    }

    /**
     * 根据请求参数和执行结果值，进行构造缓存Key
     * @param keySpEL 生成缓存Key的Spring el表达式
     * @param arguments 参数
     * @param retVal 结果值
     * @return CacheKey 缓存Key
     */
    public static String getDefinedCacheKey(String keySpEL, Object[] arguments, Object retVal) {
        if(keySpEL.indexOf("#" + ARGS) != -1 || keySpEL.indexOf("#" + RET_VAL) != -1) {
            return getElValue(keySpEL, arguments, retVal, String.class);
        } else {
            return keySpEL;
        }
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
        if(null != arguments && arguments.length > 0 && null != cache.condition() && cache.condition().length() > 0) {
            rv=getElValue(cache.condition(), arguments, result, Boolean.class);
        }
        return rv;
    }

    /**
     * 是否可以自动加载
     * @param cache Cache 注解
     * @param arguments 参数
     * @return autoload 是否自动加载
     */
    public static boolean isAutoload(Cache cache, Object[] arguments) {
        boolean autoload=cache.autoload();
        if(null != arguments && arguments.length > 0 && null != cache.autoloadCondition() && cache.autoloadCondition().length() > 0) {
            autoload=getElValue(cache.autoloadCondition(), arguments, Boolean.class);
        }
        return autoload && cache.expire() > 120;
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
            rv=getElValue(cacheDeleteKey.condition(), arguments, retVal, Boolean.class);
        }
        return rv;
    }
}
