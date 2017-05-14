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

import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import org.apache.curator.universal.modeled.Node;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.modeled.ModelSpecBuilder;
import org.apache.curator.x.async.modeled.ModeledFramework;
import org.apache.curator.x.async.modeled.ZPath;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

class ModeledHandleImpl<T> implements ModeledHandle<T>
{
    private final ModeledFramework<T> client;

    ModeledHandleImpl(AsyncCuratorFramework client, ModelSpec<T> modelSpec)
    {
        modelSpec = Objects.requireNonNull(modelSpec, "modelSpec cannot be null");
        ModelSpecBuilder<T> builder = org.apache.curator.x.async.modeled.ModelSpec.builder(asZPath(modelSpec.path()), SerializerDelegate.wrap(modelSpec.serializer()));
        org.apache.curator.x.async.modeled.ModelSpec<T> curatorModelSpec = builder.build();
        this.client = ModeledFramework.wrap(client, curatorModelSpec);
    }

    static ZPath asZPath(NodePath path)
    {
        return ZPath.parseWithIds(path.toString());    // TODO always use parseWithIds?
    }

    private ModeledHandleImpl(ModeledFramework<T> client)
    {
        this.client = client;
    }

    @Override
    public ModeledHandle<T> child(Object child)
    {
        return new ModeledHandleImpl<>(client.child(child));
    }

    @Override
    public ModeledHandle<T> parent()
    {
        return new ModeledHandleImpl<>(client.parent());
    }

    @Override
    public ModeledHandle<T> withPath(NodePath path)
    {
        return new ModeledHandleImpl<>(client.withPath(asZPath(path)));
    }

    @Override
    public CompletionStage<String> set(T model)
    {
        return client.set(model);
    }

    @Override
    public CompletionStage<String> set(T model, int version)
    {
        return client.set(model, version);
    }

    @Override
    public CompletionStage<T> read()
    {
        return client.read();
    }

    @Override
    public CompletionStage<Node<T>> readAsNode()
    {
        return null;    // TODO
    }

    @Override
    public CompletionStage<Void> delete()
    {
        return client.delete();
    }

    @Override
    public CompletionStage<Void> delete(int version)
    {
        return client.delete(version);
    }
}
