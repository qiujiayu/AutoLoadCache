package com.test.script;

import junit.framework.TestCase;

import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.script.JavaScriptParser;

public class JavaScriptTest extends TestCase {

    public void testJavaScript() throws Exception {
        String javaVersion=System.getProperty("java.version");
        System.out.println(javaVersion);
        int ind=0;
        for(int i=0; i < 2; i++) {
            ind=javaVersion.indexOf(".", ind);
            ind++;
        }
        javaVersion=javaVersion.substring(0, ind);
        javaVersion=javaVersion.replaceAll("\\.", "");
        System.out.println(Integer.parseInt(javaVersion));

        String keySpEL="'test_'+args[0]+'_'+args[1]";
        Object[] arguments=new Object[]{"1111", "2222"};
        AbstractScriptParser scriptParser=new JavaScriptParser();
        String res=scriptParser.getDefinedCacheKey(keySpEL, arguments, null, false);
        System.out.println(res);
     // 自定义函数使用
        Boolean rv=scriptParser.getElValue("empty(args[0])", arguments, Boolean.class);
        assertFalse(rv);
    }

}
