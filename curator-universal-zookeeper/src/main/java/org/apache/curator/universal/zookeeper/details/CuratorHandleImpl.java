package org.apache.curator.universal.zookeeper.details;

import org.apache.curator.universal.api.CuratorHandle;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import org.apache.curator.x.async.AsyncCuratorFramework;

public class CuratorHandleImpl implements CuratorHandle
{
    private final AsyncCuratorFramework client;

    public CuratorHandleImpl(AsyncCuratorFramework client)
    {
        this.client = client;
    }

    @Override
    public <T> T unwrap()
    {
        try
        {
            //noinspection unchecked
            return (T)client;
        }
        catch ( ClassCastException dummy )
        {
            // ignore
        }
        return null;
    }

    @Override
    public <T> ModeledHandle<T> wrap(ModelSpec<T> modelSpec)
    {
        return null;
    }
}
