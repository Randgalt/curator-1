package org.apache.curator.universal.zookeeper.details;

import org.apache.curator.universal.modeled.ModelSerializer;
import java.util.Objects;

class SerializerDelegate<T> implements org.apache.curator.x.async.modeled.ModelSerializer<T>
{
    private final ModelSerializer<T> serializer;

    static <T> SerializerDelegate<T> wrap(ModelSerializer<T> serializer)
    {
        return new SerializerDelegate<>(serializer);
    }

    private SerializerDelegate(ModelSerializer<T> serializer)
    {
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");
    }

    @Override
    public byte[] serialize(T model)
    {
        return serializer.serialize(model);
    }

    @Override
    public T deserialize(byte[] bytes)
    {
        return serializer.deserialize(bytes);
    }
}
