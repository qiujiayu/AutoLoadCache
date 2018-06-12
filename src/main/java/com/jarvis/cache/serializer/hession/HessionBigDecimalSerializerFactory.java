package com.jarvis.cache.serializer.hession;

import java.math.BigDecimal;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.BigDecimalDeserializer;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.StringValueSerializer;

/**
 * @author: jiayu.qiu
 */
public class HessionBigDecimalSerializerFactory extends AbstractSerializerFactory {

    private static final StringValueSerializer BIG_DECIMAL_SERIALIZER = new StringValueSerializer();

    private static final BigDecimalDeserializer BIG_DECIMAL_DESERIALIZER = new BigDecimalDeserializer();

    @Override
    public Serializer getSerializer(@SuppressWarnings("rawtypes") Class cl) throws HessianProtocolException {
        if (BigDecimal.class.isAssignableFrom(cl)) {
            return BIG_DECIMAL_SERIALIZER;
        }
        return null;
    }

    @Override
    public Deserializer getDeserializer(@SuppressWarnings("rawtypes") Class cl) throws HessianProtocolException {
        if (BigDecimal.class.isAssignableFrom(cl)) {
            return BIG_DECIMAL_DESERIALIZER;
        }
        return null;
    }

}
