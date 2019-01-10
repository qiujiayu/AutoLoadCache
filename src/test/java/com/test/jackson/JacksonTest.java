package com.test.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.Simple;
import lombok.Data;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacksonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    public static void main(String[] args) throws Exception {
        // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ///MAPPER.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        test2();
    }

    private static void test1() throws Exception {
        MethodTest methodTest = new JacksonTest.MethodTest();
        List<Simple> list = methodTest.test();
        String json = MAPPER.writeValueAsString(list);
        System.out.println(json);

        Method[] methods = MethodTest.class.getDeclaredMethods();
        Method method = null;
        for (Method tmp : methods) {
            if (tmp.getName().equals("test")) {
                method = tmp;
                break;
            }
        }
        Type type = method.getGenericReturnType();
        JavaType javaType = MAPPER.getTypeFactory().constructType(type);
        List<Simple> list2 = MAPPER.readValue(json, javaType);
        for(Simple simple:list2){
            System.out.println(simple);
        }
    }

    private static void test2() throws Exception {
        MethodTest methodTest = new JacksonTest.MethodTest();
        Map<Integer,List<Simple>> list = methodTest.test2();
        String json = MAPPER.writeValueAsString(list);
        System.out.println(json);

        Method[] methods = MethodTest.class.getDeclaredMethods();
        Method method = null;
        for (Method tmp : methods) {
            if (tmp.getName().equals("test2")) {
                method = tmp;
                break;
            }
        }
        Type type = method.getGenericReturnType();
        JavaType javaType = MAPPER.getTypeFactory().constructType(type);
        Map<Integer,List<Simple>> list2 = MAPPER.readValue(json, javaType);
        String json2 = MAPPER.writeValueAsString(list2);
        System.out.println(json2);
    }


    @Data
    static class MethodTest {

        public List<Simple> test() {
            List<Simple> list = new ArrayList<>(1);
            Simple simple = new Simple();
            simple.setAge(10);
            simple.setName("name");
            list.add(simple);
            return list;
        }

        public Map<Integer,List<Simple>> test2(){
            List<Simple> list = new ArrayList<>(1);
            Simple simple = new Simple();
            simple.setAge(10);
            simple.setName("name");
            list.add(simple);
            Map<Integer,List<Simple>> map = new HashMap<>(1);
            map.put(1, list);
            return map;
        }
    }
}
