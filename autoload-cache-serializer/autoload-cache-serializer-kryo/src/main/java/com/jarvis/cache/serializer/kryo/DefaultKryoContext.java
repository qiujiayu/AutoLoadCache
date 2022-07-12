package com.jarvis.cache.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * kryo接口默认实现
 *
 * @author stevie.wong
 */
public class DefaultKryoContext implements KryoContext {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;
    private Pool<Kryo> pool;
    private List<KryoClassRegistration> registrations;

    public static KryoContext newKryoContextFactory(KryoClassRegistration registration) {
        KryoContext kryoContext = new DefaultKryoContext();
        kryoContext.addKryoClassRegistration(registration);
        return kryoContext;
    }

    private DefaultKryoContext() {
        registrations = new ArrayList<>();

        //KryoFactory的create方法会延后调用
        /*pool = new Pool.Builder(() -> {
            Kryo kryo = new Kryo();
            registrations.forEach(reg -> reg.register(kryo));
            return kryo;
        }).softReferences().build();*/
        //FIXME 32够不够
        Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 32) {
            @Override
            protected Kryo create () {
                Kryo kryo = new Kryo();
                registrations.forEach(reg -> reg.register(kryo));
                return kryo;
            }
        };

    }

    @Override
    public byte[] serialize(Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public byte[] serialize(Object obj, int bufferSize) {
        Kryo kryo = pool.obtain();
        try (Output output = new Output(new ByteArrayOutputStream(), bufferSize)) {
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        } finally {
            pool.free(kryo);
        }
    }

    @Override
    public Object deserialize(byte[] serialized) {
        Kryo kryo = pool.obtain();
        try (Input input = new Input(new ByteArrayInputStream(serialized))) {
            Object o = kryo.readClassAndObject(input);
            return o;
        } finally {
            pool.free(kryo);
        }
    }

    @Override
    public void addKryoClassRegistration(KryoClassRegistration registration) {
        if (null != registration) {
            registrations.add(registration);
        }
    }
}
