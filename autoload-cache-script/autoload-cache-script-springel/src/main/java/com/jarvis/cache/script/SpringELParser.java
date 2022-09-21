package com.jarvis.cache.script;

import com.jarvis.cache.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring EL表达式解析处理
 *
 *
 */
public class SpringELParser extends AbstractScriptParser {

    /**
     * # 号
     */
    private static final String POUND = "#";

    /**
     * 撇号
     */
    private static final String apostrophe = "'";

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ConcurrentHashMap<String, Expression> expCache = new ConcurrentHashMap<String, Expression>();

    private static Method hash = null;

    private static Method empty = null;

    static {
        try {
            hash = CacheUtil.class.getDeclaredMethod("getUniqueHashStr", new Class[]{Object.class});
            empty = CacheUtil.class.getDeclaredMethod("isEmpty", new Class[]{Object.class});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ConcurrentHashMap<String, Method> funcs = new ConcurrentHashMap<String, Method>(8);

    /**
     * @param name   方法名
     * @param method 方法
     */
    @Override
    public void addFunction(String name, Method method) {
        funcs.put(name, method);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getElValue(String keySpEL, Object target, Object[] arguments, Object retVal, boolean hasRetVal,
                            Class<T> valueType) throws Exception {
        if (valueType.equals(String.class)) {
            // 如果不是表达式，直接返回字符串
            if (keySpEL.indexOf(POUND) == -1 && keySpEL.indexOf("'") == -1) {
                return (T) keySpEL;
            }
        }
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.registerFunction(HASH, hash);
        context.registerFunction(EMPTY, empty);
        Iterator<Map.Entry<String, Method>> it = funcs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Method> entry = it.next();
            context.registerFunction(entry.getKey(), entry.getValue());
        }
        context.setVariable(TARGET, target);
        context.setVariable(ARGS, arguments);
        if (hasRetVal) {
            context.setVariable(RET_VAL, retVal);
        }
        Expression expression = expCache.get(keySpEL);
        if (null == expression) {
            expression = parser.parseExpression(keySpEL);
            expCache.put(keySpEL, expression);
        }
        return expression.getValue(context, valueType);
    }

}
