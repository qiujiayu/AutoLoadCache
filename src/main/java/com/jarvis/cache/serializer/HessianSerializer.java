package com.jarvis.cache.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.jarvis.cache.serializer.hession.HessionBigDecimalSerializerFactory;
import com.jarvis.cache.serializer.hession.HessionSoftReferenceSerializerFactory;
import com.jarvis.lib.util.BeanUtil;

/**
 * @author jiayu.qiu
 */
public class HessianSerializer implements ISerializer<Object> {

    private static final SerializerFactory SERIALIZER_FACTORY = new SerializerFactory();

    static {
        SERIALIZER_FACTORY.addFactory(new HessionBigDecimalSerializerFactory());
        SERIALIZER_FACTORY.addFactory(new HessionSoftReferenceSerializerFactory());
    }

    /**
     * 添加自定义SerializerFactory
     * 
     * @param factory AbstractSerializerFactory
     */
    public void addSerializerFactory(AbstractSerializerFactory factory) {
        SERIALIZER_FACTORY.addFactory(factory);
    }

    @Override
    public byte[] serialize(final Object obj) throws Exception {
        if (obj == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AbstractHessianOutput output = new Hessian2Output(outputStream);
        output.setSerializerFactory(SERIALIZER_FACTORY);
        // 将对象写到流里
        output.writeObject(obj);
        output.flush();
        byte[] val = outputStream.toByteArray();
        output.close();
        return val;
    }

    @Override
    public Object deserialize(final byte[] bytes, final Type returnType) throws Exception {
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        AbstractHessianInput input = new Hessian2Input(inputStream);
        input.setSerializerFactory(SERIALIZER_FACTORY);
        Object obj = input.readObject();
        input.close();
        return obj;
    }

    @Override
    public Object deepClone(Object obj, final Type type) throws Exception {
        if (null == obj) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        if (BeanUtil.isPrimitive(obj) || clazz.isEnum() || obj instanceof Class || clazz.isAnnotation()
                || clazz.isSynthetic()) {// 常见不会被修改的数据类型
            return obj;
        }
        if (obj instanceof Date) {
            return ((Date) obj).clone();
        } else if (obj instanceof Calendar) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(((Calendar) obj).getTime().getTime());
            return cal;
        }
        return deserialize(serialize(obj), null);
    }

    @Override
    public Object[] deepCloneMethodArgs(Method method, Object[] args) throws Exception {
        if (null == args || args.length == 0) {
            return args;
        }
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (args.length != genericParameterTypes.length) {
            throw new Exception("the length of " + method.getDeclaringClass().getName() + "." + method.getName()
                    + " must " + genericParameterTypes.length);
        }
        Object[] res = new Object[args.length];
        int len = genericParameterTypes.length;
        for (int i = 0; i < len; i++) {
            res[i] = deepClone(args[i], null);
        }
        return res;
    }

}
