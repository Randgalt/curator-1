package org.apache.curator.universal.details;

import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.modeled.ModelSerializer;
import org.apache.curator.universal.modeled.ModelSpec;
import java.util.List;
import java.util.Objects;

public class ModelSpecImpl<T> implements ModelSpec<T>
{
    private final NodePath path;
    private final ModelSerializer<T> serializer;

    public ModelSpecImpl(NodePath path, ModelSerializer<T> serializer)
    {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");
    }

    @Override
    public ModelSpec<T> child(Object child)
    {
        return withPath(path.child(child));
    }

    @Override
    public ModelSpec<T> parent()
    {
        return withPath(path.parent());
    }

    @Override
    public ModelSpec<T> resolved(Object... parameters)
    {
        return withPath(path.resolved(parameters));
    }

    @Override
    public ModelSpec<T> resolved(List<Object> parameters)
    {
        return withPath(path.resolved(parameters));
    }

    @Override
    public ModelSpec<T> withPath(NodePath newPath)
    {
        return new ModelSpecImpl<>(newPath, serializer);
    }

    @Override
    public NodePath path()
    {
        return path;
    }

    @Override
    public ModelSerializer<T> serializer()
    {
        return serializer;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ModelSpecImpl<?> modelSpec = (ModelSpecImpl<?>)o;

        //noinspection SimplifiableIfStatement
        if ( !path.equals(modelSpec.path) )
        {
            return false;
        }
        return serializer.equals(modelSpec.serializer);
    }

    @Override
    public int hashCode()
    {
        int result = path.hashCode();
        result = 31 * result + serializer.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "ModelSpecImpl{" + "path=" + path + ", serializer=" + serializer + '}';
    }
}
