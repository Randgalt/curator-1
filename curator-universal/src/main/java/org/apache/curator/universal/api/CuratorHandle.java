package org.apache.curator.universal.api;

import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;

public interface CuratorHandle
{
    <T> T unwrap();

    <T> ModeledHandle<T> wrap(ModelSpec<T> modelSpec);
}
