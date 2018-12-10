package com.jarvis.cache.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * kryo接口默认实现
 *
 * @author stevie.wong
 */
@Slf4j
public class DefaultKryoContext implements KryoContext {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;
    private KryoPool pool;
    private List<KryoClassRegistration> registrations;

    public static KryoContext newKryoContextFactory(KryoClassRegistration registration) {
        KryoContext kryoContext = new DefaultKryoContext();
        kryoContext.addKryoClassRegistration(registration);
        return kryoContext;
    }

    private DefaultKryoContext() {
        registrations = Lists.newArrayList();

        //KryoFactory的create方法会延后调用
        pool = new KryoPool.Builder(() -> {
            Kryo kryo = new Kryo();
            registrations.forEach(reg -> reg.register(kryo));
            return kryo;
        }).softReferences().build();
    }

    @Override
    public byte[] serialize(Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public byte[] serialize(Object obj, int bufferSize) {
        Kryo kryo = pool.borrow();
        try (Output output = new Output(new ByteArrayOutputStream(), bufferSize)) {
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        } finally {
            pool.release(kryo);
        }
    }

    @Override
    public Object deserialize(byte[] serialized) {
        Kryo kryo = pool.borrow();
        try (Input input = new Input(new ByteArrayInputStream(serialized))) {
            Object o = kryo.readClassAndObject(input);
            return o;
        } finally {
            pool.release(kryo);
        }
    }

    @Override
    public void addKryoClassRegistration(KryoClassRegistration registration) {
        if (null != registration) {
            registrations.add(registration);
        }
    }
}
