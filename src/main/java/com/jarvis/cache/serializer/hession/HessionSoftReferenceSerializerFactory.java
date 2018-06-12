package com.jarvis.cache.serializer.hession;

import java.lang.ref.SoftReference;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;

/**
 * @author: jiayu.qiu
 */
public class HessionSoftReferenceSerializerFactory extends AbstractSerializerFactory {

    private final SoftReferenceSerializer beanSerializer = new SoftReferenceSerializer();

    private final SoftReferenceDeserializer beanDeserializer = new SoftReferenceDeserializer();

    @Override
    public Serializer getSerializer(@SuppressWarnings("rawtypes") Class cl) throws HessianProtocolException {
        if (SoftReference.class.isAssignableFrom(cl)) {
            return beanSerializer;
        }
        return null;
    }

    @Override
    public Deserializer getDeserializer(@SuppressWarnings("rawtypes") Class cl) throws HessianProtocolException {
        if (SoftReference.class.isAssignableFrom(cl)) {
            return beanDeserializer;
        }
        return null;
    }

}
