package com.test.script;

import com.jarvis.cache.script.IScriptParser;
import com.jarvis.cache.script.JavaScriptParser;
import com.jarvis.cache.script.ScriptParserUtil;

public class JavaScriptTest {

    public static void main(String[] args) throws Exception {
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
