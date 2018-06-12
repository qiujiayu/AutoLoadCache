package com.jarvis.cache;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import com.jarvis.lib.util.BeanUtil;

/**
 * @author jiayu.qiu
 */
public class CacheUtil {

    private static final String SPLIT_STR = "_";

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Object obj) {
        if (null == obj) {
            return true;
        }
        if (obj instanceof String) {
            return ((String) obj).length() == 0;
        }
        Class cl = obj.getClass();
        if (cl.isArray()) {
            int len = Array.getLength(obj);
            return len == 0;
        }
        if (obj instanceof Collection) {
            Collection tempCol = (Collection) obj;
            return tempCol.isEmpty();
        }
        if (obj instanceof Map) {
            Map tempMap = (Map) obj;
            return tempMap.isEmpty();
        }
        return false;
    }

    /**
     * 生成字符串的HashCode
     * 
     * @param buf
     * @return int hashCode
     */
    private static int getHashCode(String buf) {
        int hash = 5381;
        int len = buf.length();

        while (len-- > 0) {
            hash = ((hash << 5) + hash) + buf.charAt(len);
        }
        return hash;
    }

    /**
     * 将Object 对象转换为唯一的Hash字符串
     * 
     * @param obj Object
     * @return Hash字符串
     */
    public static String getUniqueHashStr(Object obj) {
        return getMiscHashCode(BeanUtil.toString(obj));
    }

    /**
     * 通过混合Hash算法，将长字符串转为短字符串（字符串长度小于等于20时，不做处理）
     * 
     * @param str String
     * @return Hash字符串
     */
    public static String getMiscHashCode(String str) {
        if (null == str || str.length() == 0) {
            return "";
        }
        int originSize = 20;
        if (str.length() <= originSize) {
            return str;
        }
        StringBuilder tmp = new StringBuilder();
        tmp.append(str.hashCode()).append(SPLIT_STR).append(getHashCode(str));

        int mid = str.length() / 2;
        String str1 = str.substring(0, mid);
        String str2 = str.substring(mid);
        tmp.append(SPLIT_STR).append(str1.hashCode());
        tmp.append(SPLIT_STR).append(str2.hashCode());

        return tmp.toString();
    }

    /**
     * 生成缓存Key
     * 
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @return CacheKey 缓存Key
     */
    public static String getDefaultCacheKey(String className, String method, Object[] arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDefaultCacheKeyPrefix(className, method, arguments));
        if (null != arguments && arguments.length > 0) {
            sb.append(getUniqueHashStr(arguments));
        }
        return sb.toString();
    }

    /**
     * 生成缓存Key的前缀
     * 
     * @param className 类名称
     * @param method 方法名称
     * @param arguments 参数
     * @return CacheKey 缓存Key
     */
    public static String getDefaultCacheKeyPrefix(String className, String method, Object[] arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        if (null != method && method.length() > 0) {
            sb.append(".").append(method);
        }
        return sb.toString();
    }

}
