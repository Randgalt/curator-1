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
package org.apache.curator.universal.zookeeper.details;

import org.apache.curator.universal.api.CuratorHandle;
import org.apache.curator.universal.api.Node;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.api.SessionState;
import org.apache.curator.universal.api.SessionStateListener;
import org.apache.curator.universal.cache.CuratorCache;
import org.apache.curator.universal.listening.Listenable;
import org.apache.curator.universal.locks.CuratorLock;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import org.apache.curator.x.async.AsyncCuratorFramework;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class CuratorHandleImpl implements CuratorHandle
{
    private final AsyncCuratorFramework client;

    public CuratorHandleImpl(AsyncCuratorFramework client)
    {
        this.client = client;
    }

    @Override
    public Listenable<SessionStateListener> sessionStateListenable()
    {
        return null;
    }

    @Override
    public <T> ModeledHandle<T> wrap(ModelSpec<T> modelSpec)
    {
        return null;
    }

    @Override
    public CuratorLock createLock(NodePath lockPath)
    {
        return null;
    }

    @Override
    public SessionState sessionState()
    {
        return null;
    }

    @Override
    public CuratorCache newCuratorCache(NodePath path)
    {
        return null;
    }

    @Override
    public boolean blockUntilSession(Duration maxBlock)
    {
        return false;
    }

    @Override
    public CompletionStage<Node<byte[]>> read(NodePath path)
    {
        return null;
    }

    @Override
    public CompletionStage<Void> set(NodePath path, byte[] data)
    {
        return null;
    }

    @Override
    public CompletionStage<Void> set(NodePath path, int version, byte[] data)
    {
        return null;
    }

    @Override
    public CompletionStage<Void> delete(NodePath path)
    {
        return null;
    }

    @Override
    public CompletionStage<Void> delete(NodePath path, int version)
    {
        return null;
    }

    @Override
    public CompletionStage<List<NodePath>> children(NodePath path)
    {
        return null;
    }
}
