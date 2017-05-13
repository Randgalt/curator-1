package org.apache.curator.universal.modeled;

/**
 * Serializing interface for models
 */
public interface ModelSerializer<T>
{
    /**
     * Given a model return the serialized bytes
     *
     * @param model model
     * @return bytes
     */
    byte[] serialize(T model);

    /**
     * Given bytes serialized via {@link #serialize(Object)} return
     * the model
     *
     * @param bytes serialized bytes
     * @return model
     * @throws RuntimeException if <code>bytes</code> is invalid or there was an error deserializing
     */
    T deserialize(byte[] bytes);

    /**
     * Simple pass-through for using byte arrays directly
     *
     * @return pass through serializer
     */
    static ModelSerializer<byte[]> raw()
    {
        return new ModelSerializer<byte[]>()
        {
            @Override
            public byte[] serialize(byte[] bytes)
            {
                return bytes;
            }

            @Override
            public byte[] deserialize(byte[] bytes)
            {
                return bytes;
            }
        };
    }
}
