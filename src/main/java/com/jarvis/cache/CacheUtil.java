package com.jarvis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.jarvis.lib.util.BeanUtil;

/**
 * @author jiayu.qiu
 */
public class CacheUtil {

    private static final Logger logger=Logger.getLogger(CacheUtil.class);

    private static final String SPLIT_STR="_";

    private static final Map<String, Long> processing=new ConcurrentHashMap<String, Long>();

    private static final ReentrantLock lock=new ReentrantLock();

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
     * 生成缓存Key
     * @param className 类名称
     * @param method  方法名称
     * @param arguments 参数
     * @return
     */
    public static String getCahcaheKey(String className, String method, Object[] arguments) {
        StringBuilder sb=new StringBuilder();
        sb.append(getCacheKeyPrefix(className, method, arguments, null));
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
    public static String getCahcaheKey(String className, String method, Object[] arguments, String subKeySpEL) {
        StringBuilder sb=new StringBuilder();
        sb.append(getCacheKeyPrefix(className, method, arguments, subKeySpEL));
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
    public static String getCacheKeyPrefix(String className, String method, Object[] arguments, String subKeySpEL){
        StringBuilder sb=new StringBuilder();
        sb.append(className).append(".").append(method);
        if(null != arguments && arguments.length>0 && null != subKeySpEL && subKeySpEL.trim().length() > 0) {
            ExpressionParser parser=new SpelExpressionParser();
            EvaluationContext context=new StandardEvaluationContext();
            context.setVariable("args", arguments);
            String subKey=parser.parseExpression(subKeySpEL).getValue(context, String.class);
            if(null != subKey && subKey.trim().length() > 0) {
                sb.append(".").append(subKey);
            }
        }
        sb.append(":");
        return sb.toString();
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
    
    private static boolean isCacheable(Cache cahce, Object[] arguments){
        boolean rv=true;
        if(null != arguments && arguments.length>0 && null != cahce.condition() && cahce.condition().length() > 0) {
            ExpressionParser parser=new SpelExpressionParser();
            EvaluationContext context=new StandardEvaluationContext();
            context.setVariable("args", arguments);
            rv=parser.parseExpression(cahce.condition()).getValue(context, Boolean.class);
        }
        return rv;
    }
    /**
     * 通过ProceedingJoinPoint，去缓存中获取数据，或从ProceedingJoinPoint中获取数据
     * @param pjp
     * @param cahce
     * @param autoLoadHandler
     * @param cacheGeterSeter
     * @return
     * @throws Exception
     */
    public static <T> T proceed(ProceedingJoinPoint pjp, Cache cahce, AutoLoadHandler<T> autoLoadHandler, CacheGeterSeter<T> cacheGeterSeter)
        throws Exception {
        Object[] arguments=pjp.getArgs();
        if(!isCacheable(cahce, arguments)) {// 如果不进行缓存，则直接返回数据
            try {
                @SuppressWarnings("unchecked")
                T result=(T)pjp.proceed();
                return result;
            } catch(Exception e) {
                throw e;
            } catch(Throwable e) {
                throw new Exception(e);
            }
        }
        int expire=cahce.expire();
        if(expire <= 0) {
            expire=300;
        }

        String className=pjp.getTarget().getClass().getName();
        String methodName=pjp.getSignature().getName();
        String cacheKey=getCahcaheKey(className, methodName, arguments, cahce.subKeySpEL());
        AutoLoadTO autoLoadTO=null;
        if(cahce.autoload()) {
            try {
                autoLoadTO=autoLoadHandler.getAutoLoadTO(cacheKey);
                if(null == autoLoadTO) {
                    arguments=(Object[])BeanUtil.deepClone(arguments);
                    autoLoadTO=new AutoLoadTO(cacheKey, pjp, arguments, expire, cahce.requestTimeout());
                    autoLoadHandler.setAutoLoadTO(autoLoadTO);
                }
                autoLoadTO.setLastRequestTime(System.currentTimeMillis());
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        CacheWrapper<T> cacheWrapper=cacheGeterSeter.get(cacheKey);
        if(null != cacheWrapper) {
            if(null != autoLoadTO && cacheWrapper.getLastLoadTime() > autoLoadTO.getLastLoadTime()) {
                autoLoadTO.setLastLoadTime(cacheWrapper.getLastLoadTime());
            }
            return cacheWrapper.getCacheObject();
        }

        Long lastProcTime=null;
        try {
            lock.lock();
            lastProcTime=processing.get(cacheKey);// 为发减少数据层的并发，增加等待机制。
            if(null == lastProcTime) {
                processing.put(cacheKey, System.currentTimeMillis());
            }
        } finally {
            lock.unlock();
        }
        if(null == lastProcTime) {
            return loadData(pjp, autoLoadTO, cacheKey, cacheGeterSeter, expire);
        }
        long startWait=System.currentTimeMillis();
        while(System.currentTimeMillis() - startWait < 300) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch(InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            cacheWrapper=cacheGeterSeter.get(cacheKey);
            if(cacheWrapper != null) {
                break;
            }
        }
        if(null == cacheWrapper) {
            return loadData(pjp, autoLoadTO, cacheKey, cacheGeterSeter, expire);
        }
        return cacheWrapper.getCacheObject();
    }
    
    /**
     * 通过ProceedingJoinPoint加载数据
     * @param pjp
     * @param autoLoadTO
     * @param cacheKey
     * @param cacheGeterSeter
     * @param expire
     * @return
     * @throws Exception
     */
    private static <T> T loadData(ProceedingJoinPoint pjp, AutoLoadTO autoLoadTO, String cacheKey, CacheGeterSeter<T> cacheGeterSeter,
        int expire) throws Exception {
        try {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(true);
            }
            long startTime=System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            T result=(T)pjp.proceed();
            long useTime=System.currentTimeMillis() - startTime;
            if(useTime >= 500) {
                String className=pjp.getTarget().getClass().getName();
                logger.error(className + "." + pjp.getSignature().getName() + ", use time:" + useTime + "ms");
            }
            CacheWrapper<T> tmp=new CacheWrapper<T>();
            tmp.setCacheObject(result);
            tmp.setLastLoadTime(System.currentTimeMillis());
            cacheGeterSeter.setCache(cacheKey, tmp, expire);
            if(null != autoLoadTO) {
                autoLoadTO.setLastLoadTime(startTime);
            }
            return result;
        } catch(Exception e) {
            throw e;
        } catch(Throwable e) {
            throw new Exception(e);
        } finally {
            if(null != autoLoadTO) {
                autoLoadTO.setLoading(false);
            }
            synchronized(lock) {
                lock.notifyAll();
            }
            processing.remove(cacheKey);
        }
    }
}
