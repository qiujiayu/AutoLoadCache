package com.jarvis.cache.serializer;

import java.math.BigDecimal;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.BigDecimalDeserializer;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.StringValueSerializer;

public class HessionBigDecimalSerializerFactory extends AbstractSerializerFactory {

    private static final StringValueSerializer bigDecimalSerializer=new StringValueSerializer();

    private static final BigDecimalDeserializer bigDecimalDeserializer=new BigDecimalDeserializer();

    @Override
    public Serializer getSerializer(@SuppressWarnings("rawtypes") Class cl) throws HessianProtocolException {
        if(BigDecimal.class.isAssignableFrom(cl)) {
            return bigDecimalSerializer;
        }
        return null;
    }

    @Override
    public Deserializer getDeserializer(@SuppressWarnings("rawtypes") Class cl) throws HessianProtocolException {
        if(BigDecimal.class.isAssignableFrom(cl)) {
            return bigDecimalDeserializer;
        }
        return null;
    }

}
