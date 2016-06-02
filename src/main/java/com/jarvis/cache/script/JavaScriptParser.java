package com.jarvis.cache.script;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.apache.log4j.Logger;

import com.jarvis.cache.CacheUtil;

/**
 * 解析JavaScript表达式
 * @author jiayu.qiu
 */
public class JavaScriptParser implements IScriptParser {

    private static final Logger logger=Logger.getLogger(JavaScriptParser.class);

    private static final ScriptEngineManager manager=new ScriptEngineManager();

    private static final ScriptEngine engine=manager.getEngineByName("javascript");

    private final ConcurrentHashMap<String, CompiledScript> expCache=new ConcurrentHashMap<String, CompiledScript>();

    private static final StringBuffer funcs=new StringBuffer();
    static {
        try {
            registerFunction(HASH, CacheUtil.class.getDeclaredMethod("getUniqueHashStr", new Class[]{Object.class}));
            registerFunction(EMPTY, CacheUtil.class.getDeclaredMethod("isEmpty", new Class[]{Object.class}));
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private static void registerFunction(String name, Method method) {
        try {
            String clsName=method.getDeclaringClass().getName();
            String methodName=method.getName();
            funcs.append("function " + name + "(obj){return " + clsName + "." + methodName + "(obj);}");
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void addFunction(String name, Method method) {
        registerFunction(name, method);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getElValue(String exp, Object[] arguments, Object retVal, boolean hasRetVal, Class<T> valueType) throws Exception {
        Bindings bindings=new SimpleBindings();
        bindings.put(ARGS, arguments);
        if(hasRetVal) {
            bindings.put(RET_VAL, retVal);
        }
        CompiledScript script=expCache.get(exp);
        if(null != script) {
            return (T)script.eval(bindings);
        }
        if(engine instanceof Compilable) {
            Compilable compEngine=(Compilable)engine;
            script=compEngine.compile(funcs + exp);
            expCache.put(exp, script);
            return (T)script.eval(bindings);
        } else {
            return (T)engine.eval(funcs + exp, bindings);
        }
    }

}
