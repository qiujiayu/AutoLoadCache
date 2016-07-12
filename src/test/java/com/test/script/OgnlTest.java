package com.test.script;

import junit.framework.TestCase;

import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.script.OgnlParser;

public class OgnlTest extends TestCase {

    public void testJavaScript() throws Exception {

        String keySpEL="'test_'+#args[0]+'_'+#args[1]";
        Object[] arguments=new Object[]{"1111", "2222"};
        AbstractScriptParser scriptParser=new OgnlParser();
        String res=scriptParser.getDefinedCacheKey(keySpEL, arguments, null, false);
        System.out.println(res);

        Boolean rv=scriptParser.getElValue("@@empty(#args[0])", arguments, Boolean.class);
        assertFalse(rv);
    }

}
