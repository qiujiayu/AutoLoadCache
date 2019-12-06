package com.test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: jiayu.qiu
 * @date: 2019年07月04日 21时21分
 */
public class ReflectTest {

    public List<String> getList() {
        return new ArrayList<>();
    }

    public String[] getArray() {
        return new String[1];
    }

    public static void main(String[] args) throws Exception {
        Method listMethod = ReflectTest.class.getMethod("getList");
        Class<?> listClass = listMethod.getReturnType();
        Class<?> listComp = listClass.getComponentType();
        Type listType = listMethod.getGenericReturnType();

        Method arrayMethod = ReflectTest.class.getMethod("getArray");
        Class<?> arrayClass = arrayMethod.getReturnType();
        Class<?> arrComp = arrayClass.getComponentType();
        Type arrayType = arrayMethod.getGenericReturnType();
    }
}
