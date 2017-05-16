/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.curator.universal.consul.details;

import com.google.common.base.Preconditions;
import org.apache.curator.universal.api.CuratorHandle;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.api.SessionState;
import org.apache.curator.universal.consul.client.ConsulClient;
import org.apache.curator.universal.locks.CuratorLock;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import java.util.Objects;

public class ConsulCuratorHandle implements CuratorHandle
{
    private final ConsulClientImpl consulClient;

    public ConsulCuratorHandle(ConsulClient consulClient)
    {
        Preconditions.checkArgument(consulClient instanceof ConsulClientImpl, "ConsulClient not created properly"); // TODO
        this.consulClient = (ConsulClientImpl)Objects.requireNonNull(consulClient, "consulClient cannot be null");
    }

    @Override
    public <T> T unwrap()
    {
        try
        {
            //noinspection unchecked
            return (T)consulClient;
        }
        catch ( ClassCastException dummy )
        {
            // TODO ignore
        }
        return null;
    }

    @Override
    public <T> ModeledHandle<T> wrap(ModelSpec<T> modelSpec)
    {
        return new ModeledHandleImpl<>(consulClient, modelSpec);
    }

    @Override
    public CuratorLock createLock(NodePath lockPath)
    {
        return new CuratorLockImpl(consulClient, Objects.requireNonNull(lockPath, "lockPath cannot be null"));
    }

    @Override
    public SessionState sessionState()
    {
        return null;
    }
}
