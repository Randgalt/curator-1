package org.apache.curator.universal.zookeeper;

import org.apache.curator.universal.api.CuratorHandle;
import org.apache.curator.universal.zookeeper.details.CuratorHandleImpl;
import org.apache.curator.x.async.AsyncCuratorFramework;
import java.util.Objects;

public interface CuratorHandleFactory
{
    static CuratorHandle wrap(AsyncCuratorFramework client)
    {
        return new CuratorHandleImpl(Objects.requireNonNull(client, "client cannot be null"));
    }
}
