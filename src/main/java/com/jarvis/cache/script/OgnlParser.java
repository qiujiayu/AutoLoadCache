package com.jarvis.cache.script;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jarvis.cache.CacheUtil;

import ognl.Ognl;
import ognl.OgnlContext;

/**
 * 解析JavaScript表达式
 * @author jiayu.qiu
 */
public class OgnlParser extends AbstractScriptParser {

    private final ConcurrentHashMap<String, Object> expCache=new ConcurrentHashMap<String, Object>();

    private final ConcurrentHashMap<String, Class<?>> funcs=new ConcurrentHashMap<String, Class<?>>(64);

    public OgnlParser() {
    }

    @Override
    public void addFunction(String name, Method method) {
        funcs.put(name, method.getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getElValue(String exp, Object[] arguments, Object retVal, boolean hasRetVal, Class<T> valueType) throws Exception {
        Object object=expCache.get(exp);
        if(null == object) {
            String className=CacheUtil.class.getName();
            String exp2=exp.replaceAll("@@" + HASH + "\\(", "@" + className + "@getUniqueHashStr(");
            exp2=exp2.replaceAll("@@" + EMPTY + "\\(", "@" + className + "@isEmpty(");

            Iterator<Map.Entry<String, Class<?>>> it=funcs.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String, Class<?>> entry=it.next();
                className=entry.getValue().getName();
                exp2=exp2.replaceAll("@@" + entry.getKey() + "\\(", "@" + className + "@" + entry.getKey() + "(");
            }
            object=Ognl.parseExpression(exp2);
            expCache.put(exp, object);
        }
        OgnlContext context=new OgnlContext();
        context.put(ARGS, arguments);
        if(hasRetVal) {
            context.put(RET_VAL, retVal);
        }
        context.setRoot(arguments);
        Object res=Ognl.getValue(object, context, context.getRoot(), valueType);
        return (T)res;
    }
}
