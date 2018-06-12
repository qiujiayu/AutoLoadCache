package com.jarvis.cache.serializer.hession;

import java.io.IOException;
import java.lang.ref.SoftReference;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializer;
import com.caucho.hessian.io.ObjectSerializer;
import com.caucho.hessian.io.Serializer;

/**
 * @author: jiayu.qiu
 */
public class SoftReferenceSerializer extends AbstractSerializer implements ObjectSerializer {

    @Override
    public Serializer getObjectSerializer() {
        return this;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (out.addRef(obj)) {
            return;
        }
        @SuppressWarnings("unchecked")
        SoftReference<Object> data = (SoftReference<Object>) obj;

        int refV = out.writeObjectBegin(SoftReference.class.getName());

        if (refV == -1) {
            out.writeInt(1);
            out.writeString("ref");
            out.writeObjectBegin(SoftReference.class.getName());
        }
        if (data != null) {
            Object ref = data.get();
            if (null != ref) {
                out.writeObject(ref);
            } else {
                out.writeNull();
            }
        } else {
            out.writeNull();
        }
    }
}
