package com.jarvis.cache;

/**
 * 缓存批处理：更新、清理
 *
 * @author hailin0@yeah.net
 * @date 2017-3-23 22:32
 */
public final class CacheUpdateHandler {

    /**
     * 静态化唯一实例、方便调用
     */
    private static final CacheUpdateHandler handler = new CacheUpdateHandler();

    /**
     * 操作
     */
    private static ThreadLocal<Operation> mark = new ThreadLocal<Operation>();

    private CacheUpdateHandler(){}

    /**
     * 标记操作
     *
     * @param operation
     */
    public static CacheUpdateHandler mark(Operation operation) {
        if (operation == null)
            throw new RuntimeException("mark set operation is not null !");
        if (mark.get() != null)
            throw new RuntimeException("mark is exist !");

        mark.set(operation);

        //如此类使用频繁,可以使用静态化的唯一实例
        return handler;

        //每次生成实例
        //return new CacheUpdateHandler();
    }

    /**
     * 处理的batchs列表，实际为吊起aop流程执行
     *
     * @param obj 缓存方法执行结果
     */
    public void batchs(Object... obj) {
        //卸载操作标记
        mark.remove();
    }

    /**
     * 判断标记，是否清理流程
     *
     * @return
     */
    protected static boolean isClear() {
        return mark.get() == Operation.CLEAR;
    }

    /**
     * 判断标记，是否更新流程
     *
     * @return
     */
    protected static boolean isUpdate() {
        return mark.get() == Operation.UPDATE;
    }

    /**
     * 缓存处理包装类
     */
    public static abstract class CacheBatchObj {
        public CacheBatchObj() {
            exe();
        }

        public abstract void exe();
    }

    /**
     * 操作类型
     */
    public static enum Operation {
        CLEAR,//清理
        UPDATE;//更新
    }


}
