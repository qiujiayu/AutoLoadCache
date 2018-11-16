package com.jarvis.cache.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * kryo接口默认实现
 *
 * @author stevie.wong
 */
@Slf4j
public class DefaultKryoContext implements KryoContext {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;
    private KryoPool pool;

    public static KryoContext newKryoContextFactory(KryoClassRegistration registration)
    {
        return new DefaultKryoContext(registration);
    }

    private DefaultKryoContext(KryoClassRegistration registration)
    {
        KryoFactory factory = new KryoFactoryImpl(registration);

        pool = new KryoPool.Builder(factory).softReferences().build();
    }

    private static class KryoFactoryImpl implements KryoFactory
    {
        private KryoClassRegistration registration;

        public KryoFactoryImpl(KryoClassRegistration registration)
        {
            this.registration = registration;
        }

        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();

            if (registration != null) {
                registration.register(kryo);
            }

            return kryo;
        }
    }

    @Override
    public byte[] serialize(Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public byte[] serialize(Object obj, int bufferSize) {
        Kryo kryo = pool.borrow();
        try(Output output = new Output(new ByteArrayOutputStream(), bufferSize))
        {
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        }
        finally {
            pool.release(kryo);
        }
    }

    @Override
    public Object deserialize(byte[] serialized) {
        Kryo kryo = pool.borrow();
        try (Input input = new Input(new ByteArrayInputStream(serialized)))
        {
            Object o = kryo.readClassAndObject(input);
            return o;
        }
        finally {
            pool.release(kryo);
        }
    }
}
