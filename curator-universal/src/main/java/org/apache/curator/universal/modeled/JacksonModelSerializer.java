package org.apache.curator.universal.modeled;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Model serializer that uses Jackson for JSON serialization. <strong>IMPORTANT: </strong>
 * the jackson dependency is specified as <code>provided</code> in the curator-x-async Maven POM
 * file to avoid adding a new dependency to Curator. Therefore, if you wish to use the
 * JacksonModelSerializer you must manually add the dependency to your build system
 */
public class JacksonModelSerializer<T> implements ModelSerializer<T>
{
    private static final ObjectMapper mapper = new ObjectMapper();
    static
    {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final ObjectReader reader;
    private final ObjectWriter writer;

    public static <T> JacksonModelSerializer<T> build(Class<T> modelClass)
    {
        return new JacksonModelSerializer<>(modelClass);
    }

    public static <T> JacksonModelSerializer<T> build(JavaType type)
    {
        return new JacksonModelSerializer<>(type);
    }

    public static <T> JacksonModelSerializer<T> build(TypeReference type)
    {
        return new JacksonModelSerializer<>(type);
    }

    public JacksonModelSerializer(Class<T> modelClass)
    {
        this(mapper.getTypeFactory().constructType(modelClass));
    }

    public JacksonModelSerializer(JavaType type)
    {
        reader = mapper.readerFor(type);
        writer = mapper.writerFor(type);
    }

    public JacksonModelSerializer(TypeReference type)
    {
        reader = mapper.readerFor(type);
        writer = mapper.writerFor(type);
    }

    public JacksonModelSerializer(ObjectMapper mapper, JavaType type)
    {
        reader = mapper.readerFor(type);
        writer = mapper.writerFor(type);
    }

    public JacksonModelSerializer(ObjectMapper mapper, TypeReference type)
    {
        reader = mapper.readerFor(type);
        writer = mapper.writerFor(type);
    }

    public JacksonModelSerializer(ObjectReader reader, ObjectWriter writer)
    {
        this.reader = Objects.requireNonNull(reader, "reader cannot be null");
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
    }

    @Override
    public byte[] serialize(T model)
    {
        try
        {
            return writer.writeValueAsBytes(model);
        }
        catch ( JsonProcessingException e )
        {
            throw new RuntimeException(String.format("Could not serialize value: %s", model), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes)
    {
        try
        {
            return reader.readValue(bytes);
        }
        catch ( IOException e )
        {
            throw new RuntimeException(String.format("Could not deserialize value: %s", Arrays.toString(bytes)), e);
        }
    }
}
