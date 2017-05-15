package org.apache.curator.universal.details;

import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.modeled.ModelSerializer;
import org.apache.curator.universal.modeled.ModelSpec;
import java.util.Objects;

public class ModelSpecBuilder<T>
{
    private final ModelSerializer<T> serializer;
    private NodePath path;

    public ModelSpecBuilder(ModelSerializer<T> serializer)
    {
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");
    }

    public ModelSpecBuilder(NodePath path, ModelSerializer<T> serializer)
    {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");
    }

    /**
     * Build a new ModelSpec instance
     *
     * @return new ModelSpec instance
     */
    public ModelSpec<T> build()
    {
        return new ModelSpecImpl<>(path, serializer);
    }

    /**
     * Change the model spec's path
     *
     * @param path new path
     * @return this for chaining
     */
    public ModelSpecBuilder<T> withPath(NodePath path)
    {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        return this;
    }
}
