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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.universal.api.Metadata;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.consul.client.ConsulClient;
import org.apache.curator.universal.modeled.CachedModeledHandle;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import org.apache.curator.universal.api.Node;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

class ModeledHandleImpl<T> implements ModeledHandle<T>
{
    private final ModelSpec<T> modelSpec;
    private final ConsulClientImpl consulClient;

    ModeledHandleImpl(ConsulClientImpl consulClient, ModelSpec<T> modelSpec)
    {
        this.consulClient = consulClient;
        this.modelSpec = Objects.requireNonNull(modelSpec, "modelSpec cannot be null");
    }

    @Override
    public ModelSpec<T> modelSpec()
    {
        return modelSpec;
    }

    @Override
    public CachedModeledHandle<T> cached()
    {
        return null;
    }

    @Override
    public CachedModeledHandle<T> cached(ExecutorService executor)
    {
        return null;
    }

    @Override
    public ModeledHandle<T> child(Object child)
    {
        return new ModeledHandleImpl<>(consulClient, modelSpec.child(child));
    }

    @Override
    public ModeledHandle<T> parent()
    {
        return new ModeledHandleImpl<>(consulClient, modelSpec.parent());
    }

    @Override
    public ModeledHandle<T> withPath(NodePath path)
    {
        return new ModeledHandleImpl<>(consulClient, modelSpec.withPath(path));
    }

    @Override
    public CompletionStage<String> set(T model)
    {
        return set(model, -1);
    }

    @Override
    public CompletionStage<String> set(T model, int version)
    {
        byte[] bytes;
        try
        {
            bytes = modelSpec.serializer().serialize(model);
        }
        catch ( Exception e )
        {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
        CompletionStage<JsonNode> stage = (version >= 0) ? consulClient.set(modelSpec.path(), version, bytes) : consulClient.set(modelSpec.path(), bytes);
        return stage.thenApply(__ -> null);
    }

    @Override
    public CompletionStage<T> read()
    {
        return readAsNode().thenApply(Node::value);
    }

    @Override
    public CompletionStage<Node<T>> readAsNode()
    {
        return consulClient.read(modelSpec.path()).thenApply(node -> {
            JsonNode firstChild = Json.requireFirstChild(node);
            byte[] data = Base64.getDecoder().decode(firstChild.get("Value").asText());
            T model = modelSpec.serializer().deserialize(data);
            return new Node<T>()
            {
                @Override
                public NodePath path()
                {
                    return modelSpec.path();
                }

                @Override
                public Metadata metadata()
                {
                    return new MetadataImpl(node.get("ModifyIndex").asInt());
                }

                @Override
                public T value()
                {
                    return model;
                }
            };
        });
    }

    @Override
    public CompletionStage<Void> delete()
    {
        return consulClient.delete(modelSpec.path()).thenAccept(__ -> {});
    }

    @Override
    public CompletionStage<Void> delete(int version)
    {
        return consulClient.delete(modelSpec.path(), version).thenAccept(__ -> {});
    }

    @Override
    public CompletionStage<List<NodePath>> children()
    {
        return consulClient.children(modelSpec.path());
    }

    @Override
    public CompletionStage<List<NodePath>> siblings()
    {
        return consulClient.children(modelSpec.parent().path());
    }

    ConsulClientImpl consulClient()
    {
        return consulClient;
    }
}
