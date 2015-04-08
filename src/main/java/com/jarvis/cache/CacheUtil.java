package com.jarvis.cache;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.lib.util.BeanUtil;

/**
 * @author jiayu.qiu
 */
public class CacheUtil {

    private static final String SPLIT_STR="_";

    private static final String ARGS="args";

    private static final ExpressionParser parser=new SpelExpressionParser();

    /**
     * 生成字符串的HashCode
     * @param buf
     * @return
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
     * 通过Hash算法，将长字符串转为短字符串
     * @param str
     * @return
     */
    public static String getMiscHashCode(String str) {
        if(null == str || str.length() == 0) {
            return "";
        }
        StringBuilder tmp=new StringBuilder();
        tmp.append(str.hashCode()).append(SPLIT_STR).append(getHashCode(str));
        if(str.length() >= 2) {
            int mid=str.length() / 2;
            String str1=str.substring(0, mid);
            String str2=str.substring(mid);
            tmp.append(SPLIT_STR).append(str1.hashCode());
            tmp.append(SPLIT_STR).append(str2.hashCode());
        }
        return tmp.toString();
    }

    public static <T> T getElValue(String keySpEL, Object[] arguments, Class<T> valueType) {
        EvaluationContext context=new StandardEvaluationContext();
        context.setVariable(ARGS, arguments);
        return parser.parseExpression(keySpEL).getValue(context, valueType);
    }

    /**
     * 生成自定义缓存Key
     * @param keySpEL
     * @param arguments
     * @return
     */
    public static String getDefinedCacheKey(String keySpEL, Object[] arguments) {
        if(keySpEL.indexOf("#" + ARGS) != -1) {
            return getElValue(keySpEL, arguments, String.class);
        } else {
            return keySpEL;
        }
    }

    /**
     * 生成缓存Key
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @return
     */
    public static String getDefaultCacheKey(String className, String method, Object[] arguments) {
        StringBuilder sb=new StringBuilder();
        sb.append(getDefaultCacheKeyPrefix(className, method, arguments, null));
        if(null != arguments && arguments.length > 0) {
            StringBuilder arg=new StringBuilder();
            for(Object obj: arguments) {
                arg.append(BeanUtil.toString(obj));
            }
            sb.append(getMiscHashCode(arg.toString()));
        }
        return sb.toString();
    }

    /**
     * 生成缓存Key
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @param subKeySpEL SpringEL表达式，arguments 在SpringEL表达式中的名称为args，第一个参数为#args[0],第二个为参数为#args[1]，依此类推。
     * @return
     */
    public static String getDefaultCacheKey(String className, String method, Object[] arguments, String subKeySpEL) {
        StringBuilder sb=new StringBuilder();
        sb.append(getDefaultCacheKeyPrefix(className, method, arguments, subKeySpEL));
        if(null != arguments && arguments.length > 0) {
            StringBuilder arg=new StringBuilder();
            for(Object obj: arguments) {
                arg.append(BeanUtil.toString(obj));
            }
            sb.append(getMiscHashCode(arg.toString()));
        }
        return sb.toString();
    }

    /**
     * 生成缓存Key的前缀
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @param subKeySpEL SpringEL表达式 ，arguments 在SpringEL表达式中的名称为args，第一个参数为#args[0],第二个为参数为#args[1]，依此类推。
     * @return
     */
    public static String getDefaultCacheKeyPrefix(String className, String method, Object[] arguments, String subKeySpEL) {
        StringBuilder sb=new StringBuilder();
        sb.append(className).append(".").append(method);
        if(null != arguments && arguments.length > 0 && null != subKeySpEL && subKeySpEL.indexOf("#" + ARGS) != -1) {
            String subKey=getElValue(subKeySpEL, arguments, String.class);
            if(null != subKey && subKey.trim().length() > 0) {
                sb.append(".").append(subKey);
            }
        }
        sb.append(":");
        return sb.toString();
    }

    public static boolean isCacheable(Cache cache, Object[] arguments) {
        boolean rv=true;
        if(null != arguments && arguments.length > 0 && null != cache.condition() && cache.condition().length() > 0) {
            rv=getElValue(cache.condition(), arguments, Boolean.class);
        }
        return rv;
    }

    public static boolean isAutoload(Cache cache, Object[] arguments) {
        boolean autoload=cache.autoload();
        if(null != arguments && arguments.length > 0 && null != cache.autoloadCondition() && cache.autoloadCondition().length() > 0) {
            autoload=getElValue(cache.autoloadCondition(), arguments, Boolean.class);
        }
        return autoload;
    }
}
