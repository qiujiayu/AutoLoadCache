package com.jarvis.cache.to;

import java.io.Serializable;

import org.nutz.aop.InterceptorChain;

import com.jarvis.cache.annotation.Cache;

/**
 * 用于处理自动加载数据到缓存
 * 
 * @author jiayu.qiu
 */
public class AutoLoadTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Object args[];

	/**
	 * 缓存注解
	 */
	private Cache cache;

	/**
	 * 缓存Key
	 */
	private CacheKeyTO cacheKey;

	/**
	 * 上次从DAO加载数据时间
	 */
	private long lastLoadTime = 0L;

	/**
	 * 上次请求数据时间
	 */
	private long lastRequestTime = 0L;

	/**
	 * 第一次请求数据时间
	 */
	private long firstRequestTime = 0L;

	/**
	 * 请求数据次数
	 */
	private long requestTimes = 0L;

	private volatile boolean loading = false;

	private String cacheClass;
	private String cacheMethod;
	/**
	 * 加载次数
	 */
	private long loadCnt = 0L;

	/**
	 * 从DAO中加载数据，使用时间的总和
	 */
	private long useTotalTime = 0L;

	private transient InterceptorChain chain;

	public AutoLoadTO(CacheKeyTO cacheKey, InterceptorChain chain, Object args[], Cache cache) {
		this.cacheKey = cacheKey;
		this.cacheClass = chain.getCallingMethod().getDeclaringClass().getName();
		this.cacheMethod = chain.getCallingMethod().getName();
		this.args = args;
		this.cache = cache;
		this.chain = chain;
	}

	public InterceptorChain getChain() {
		return chain;
	}


	public long getLastRequestTime() {
		return lastRequestTime;
	}

	public void setLastRequestTime(long lastRequestTime) {
		synchronized (this) {
			this.lastRequestTime = lastRequestTime;
			if (firstRequestTime == 0) {
				firstRequestTime = lastRequestTime;
			}
			requestTimes++;
		}
	}

	public long getFirstRequestTime() {
		return firstRequestTime;
	}

	public long getRequestTimes() {
		return requestTimes;
	}

	public Cache getCache() {
		return cache;
	}

	public long getLastLoadTime() {
		return lastLoadTime;
	}

	public void setLastLoadTime(long lastLoadTime) {
		this.lastLoadTime = lastLoadTime;
	}

	public CacheKeyTO getCacheKey() {
		return cacheKey;
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public Object[] getArgs() {
		return args;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public long getLoadCnt() {
		return loadCnt;
	}

	public long getUseTotalTime() {
		return useTotalTime;
	}

	/**
	 * 记录用时
	 * 
	 * @param useTime
	 *            用时
	 */
	public void addUseTotalTime(long useTime) {
		synchronized (this) {
			this.loadCnt++;
			this.useTotalTime += useTotalTime;
		}
	}

	public String getCacheClass() {
		return cacheClass;
	}

	public String getCacheMethod() {
		return cacheMethod;
	}

	/**
	 * 平均用时
	 * 
	 * @return long 用时
	 */
	public long getAverageUseTime() {
		if (loadCnt == 0) {
			return 0;
		}
		return this.useTotalTime / this.loadCnt;
	}
}