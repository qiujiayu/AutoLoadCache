package com.jarvis.cache.script;

import com.jarvis.cache.CacheUtil;
import lombok.extern.slf4j.Slf4j;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析JavaScript表达式
 *
 *
 */
@Slf4j
public class JavaScriptParser extends AbstractScriptParser {

    private final ScriptEngineManager manager = new ScriptEngineManager();

    private final ConcurrentHashMap<String, CompiledScript> expCache = new ConcurrentHashMap<String, CompiledScript>();

    private final StringBuffer funcs = new StringBuffer();

    private static int versionCode;

    private final ScriptEngine engine;

    public JavaScriptParser() {
        engine = manager.getEngineByName("javascript");
        try {
            addFunction(HASH, CacheUtil.class.getDeclaredMethod("getUniqueHashStr", new Class[]{Object.class}));
            addFunction(EMPTY, CacheUtil.class.getDeclaredMethod("isEmpty", new Class[]{Object.class}));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void addFunction(String name, Method method) {
        try {
            String clsName = method.getDeclaringClass().getName();
            String methodName = method.getName();
            funcs.append("function " + name + "(obj){return " + clsName + "." + methodName + "(obj);}");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getElValue(String exp, Object target, Object[] arguments, Object retVal, boolean hasRetVal,
                            Class<T> valueType) throws Exception {
        Bindings bindings = new SimpleBindings();
        bindings.put(TARGET, target);
        bindings.put(ARGS, arguments);
        if (hasRetVal) {
            bindings.put(RET_VAL, retVal);
        }
        CompiledScript script = expCache.get(exp);
        if (null != script) {
            return (T) script.eval(bindings);
        }
        if (engine instanceof Compilable) {
            Compilable compEngine = (Compilable) engine;
            script = compEngine.compile(funcs + exp);
            expCache.put(exp, script);
            return (T) script.eval(bindings);
        } else {
            return (T) engine.eval(funcs + exp, bindings);
        }
    }

}
