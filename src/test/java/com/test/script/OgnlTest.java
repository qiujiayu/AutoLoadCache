package com.test.script;

import java.util.HashMap;
import java.util.Map;

import com.jarvis.cache.script.AbstractScriptParser;
import com.jarvis.cache.script.OgnlParser;
import com.test.Simple;

import junit.framework.TestCase;

/**
 * @author: jiayu.qiu
 */
public class OgnlTest extends TestCase {

    AbstractScriptParser scriptParser=new OgnlParser();

    public void testJavaScript() throws Exception {

        String keySpEL="'test_'+#args[0]+'_'+#args[1]";

        Simple simple=new Simple();
        simple.setAge(18);
        simple.setName("刘德华");
        simple.setSex(0);
        Object[] arguments=new Object[]{"1111", "2222", simple};

        String res=scriptParser.getDefinedCacheKey(keySpEL, arguments, null, false);
        System.out.println(res);
        assertEquals("test_1111_2222", res);
        // 自定义函数使用
        Boolean rv=scriptParser.getElValue("@@empty(#args[0])", arguments, Boolean.class);
        assertFalse(rv);

        String val=null;
        val=scriptParser.getElValue("@@hash(#args[0])", arguments, String.class);
        System.out.println(val);
        assertEquals("1111", val);

        val=scriptParser.getElValue("@@hash(#args[1])", arguments, String.class);
        System.out.println(val);
        assertEquals("2222", val);

        val=scriptParser.getElValue("@@hash(#args[2])", arguments, String.class);
        System.out.println(val);
        assertEquals("-290203482_-550943035_-57743508_-1052004462", val);

        val=scriptParser.getElValue("@@hash(#args)", arguments, String.class);
        System.out.println(val);
        assertEquals("322960956_-1607969343_673194431_1921252123", val);
    }

    public void testReturnIsMapWithHfield() throws Exception {

        String keySpEL="#retVal.rid";
        Object[] arguments=new Object[]{"1111", "2222"};
        Map returnObj=new HashMap(1);
        returnObj.put("rid", "iamrid");
        String res=scriptParser.getDefinedCacheKey(keySpEL, arguments, returnObj, true);
        System.out.println(res);
        assertEquals("iamrid", res);

        Simple simple=new Simple();
        simple.setAge(18);
        simple.setName("刘德华");
        simple.setSex(0);
        keySpEL="#retVal.name";

        res=scriptParser.getDefinedCacheKey(keySpEL, arguments, simple, true);
        System.out.println(res);
        assertEquals("刘德华", res);

        // 自定义函数使用
        Boolean rv=scriptParser.getElValue("@@empty(#args[0])", arguments, Boolean.class);
        assertFalse(rv);
    }
}
