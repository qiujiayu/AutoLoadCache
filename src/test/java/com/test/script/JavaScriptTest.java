package com.test.script;

import com.jarvis.cache.script.IScriptParser;
import com.jarvis.cache.script.JavaScriptParser;
import com.jarvis.cache.script.ScriptParserUtil;

public class JavaScriptTest {

    public static void main(String[] args) throws Exception {
        String keySpEL="'test'";
        Object[] arguments=new Object[]{"1111", "2222"};
        IScriptParser scriptParser=new JavaScriptParser();
        ScriptParserUtil scriptParserUtil=new ScriptParserUtil(scriptParser);
        String res=scriptParserUtil.getDefinedCacheKey(keySpEL, arguments, null, false);
        System.out.println(res);
        Boolean rv=scriptParserUtil.getElValue("empty(args[0])", arguments, Boolean.class);
        System.out.println(rv);
    }

}
