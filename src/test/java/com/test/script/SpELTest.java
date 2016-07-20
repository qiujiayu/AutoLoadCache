package com.test.script;

import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.script.SpringELParser;

public class SpELTest {

    public static void main(String[] args) throws Exception {
        String keySpEL="test";
        Object[] arguments=new Object[]{"1111", "2222"};
        AbstractScriptParser scriptParser=new SpringELParser();
        String res=scriptParser.getDefinedCacheKey(keySpEL, arguments, null, false);
        System.out.println(res);
        Boolean rv=scriptParser.getElValue("#empty(#args)", arguments, Boolean.class);
        System.out.println(rv);
    }

}
