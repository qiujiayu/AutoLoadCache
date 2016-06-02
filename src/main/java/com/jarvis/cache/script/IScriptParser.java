package com.jarvis.cache.script;

import java.lang.reflect.Method;

/**
 * 表达式处理
 * @author jiayu.qiu
 */
public interface IScriptParser {

    String ARGS="args";

    String RET_VAL="retVal";

    String HASH="hash";

    String EMPTY="empty";

    /**
     * 为了简化表达式，方便调用Java static 函数，在这里注入表达式自定义函数
     * @param name 自定义函数名
     * @param method 调用的方法
     */
    void addFunction(String name, Method method);

    /**
     * 将表达式转换期望的值
     * @param keySpEL 生成缓存Key的表达式
     * @param arguments 参数
     * @param retVal 结果值（缓存数据）
     * @param hasRetVal 是否使用 retVal 参数
     * @param valueType 表达式最终返回值类型
     * @return T value 返回值
     * @param <T> 泛型
     * @throws Exception
     */
    <T> T getElValue(String exp, Object[] arguments, Object retVal, boolean hasRetVal, Class<T> valueType) throws Exception;
}
