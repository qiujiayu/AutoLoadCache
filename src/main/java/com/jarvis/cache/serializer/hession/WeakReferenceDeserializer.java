package com.jarvis.cache.serializer.hession;

import java.io.IOException;
import java.lang.ref.WeakReference;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractMapDeserializer;
import com.caucho.hessian.io.IOExceptionWrapper;

/**
 * @author: jiayu.qiu
 */
public class WeakReferenceDeserializer extends AbstractMapDeserializer {

    @Override
    public Object readObject(AbstractHessianInput in, Object[] fields) throws IOException {
        try {
            WeakReference<Object> obj = instantiate();
            in.addRef(obj);
            Object value = in.readObject();
            obj = null;
            return new WeakReference<Object>(value);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }

    }

    protected WeakReference<Object> instantiate() throws Exception {
        Object obj = new Object();
        return new WeakReference<Object>(obj);
    }

}
