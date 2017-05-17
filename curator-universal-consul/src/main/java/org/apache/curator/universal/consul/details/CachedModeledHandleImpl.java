package org.apache.curator.universal.consul.details;

import org.apache.curator.universal.api.Node;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.cache.CuratorCacheListener;
import org.apache.curator.universal.listening.Listenable;
import org.apache.curator.universal.modeled.CachedModeledHandle;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

class CachedModeledHandleImpl<T> implements CachedModeledHandle<T>
{
    private final ModeledHandle<T> client;
    private final ConsulCacheImpl cache;
    private final Map<CuratorCacheListener<T>, CuratorCacheListener<byte[]>> listenerMap = new ConcurrentHashMap<>();
    private final Executor executor;

    CachedModeledHandleImpl(ConsulClientImpl consulClient, ModeledHandle<T> client, ExecutorService executor)
    {
        this(
            Objects.requireNonNull(client, "client cannot be null"),
            new ConsulCacheImpl(consulClient, client.modelSpec().path(), executor),
            executor
        );
    }

    private CachedModeledHandleImpl(ModeledHandle<T> client, ConsulCacheImpl cache, Executor executor)
    {
        this.client = client;
        this.cache = cache;
        this.executor = executor;
    }

    @Override
    public Executor executor()
    {
        return executor;
    }

    @Override
    public void start()
    {
        cache.start();
    }

    @Override
    public void close()
    {
        cache.close();
    }

    @Override
    public ModelSpec<T> modelSpec()
    {
        return client.modelSpec();
    }

    @Override
    public Listenable<CuratorCacheListener<T>> listenable()
    {
        return new Listenable<CuratorCacheListener<T>>()
        {
            @Override
            public void addListener(CuratorCacheListener<T> listener)
            {
                addListener(listener, Runnable::run);
            }

            @Override
            public void addListener(CuratorCacheListener<T> listener, Executor executor)
            {
                CuratorCacheListener<byte[]> bridgeListener = new CuratorCacheListener<byte[]>()
                {
                    @Override
                    public void accept(Type type, Node<byte[]> node)
                    {
                        if ( node.value().length > 0 )    // otherwise it's probably just a parent node being created
                        {
                            listener.accept(type, ModeledHandleImpl.asNode(client.modelSpec(), node));
                        }
                    }

                    @Override
                    public void initialized()
                    {
                        listener.initialized();
                    }

                    @Override
                    public void handleException(Exception e)
                    {
                        listener.handleException(e);
                    }
                };
                listenerMap.put(listener, bridgeListener);
                cache.listenable().addListener(bridgeListener, executor);
            }

            @Override
            public void removeListener(CuratorCacheListener<T> listener)
            {
                CuratorCacheListener<byte[]> bridgeListener = listenerMap.remove(listener);
                if ( bridgeListener != null )
                {
                    cache.listenable().removeListener(bridgeListener);
                }
            }
        };
    }

    @Override
    public CachedModeledHandle<T> cached()
    {
        throw new UnsupportedOperationException("Already a cached instance");
    }

    @Override
    public CachedModeledHandle<T> cached(ExecutorService executor)
    {
        throw new UnsupportedOperationException("Already a cached instance");
    }

    @Override
    public CachedModeledHandle<T> child(Object child)
    {
        return new CachedModeledHandleImpl<>(client.child(child), cache, executor);
    }

    @Override
    public ModeledHandle<T> parent()
    {
        throw new UnsupportedOperationException("Not supported for CachedModeledHandle. Instead, call parent() on the ModeledHandle before calling cached()");
    }

    @Override
    public CachedModeledHandle<T> withPath(NodePath path)
    {
        throw new UnsupportedOperationException("Not supported for CachedModeledHandle. Instead, call withPath() on the ModeledHandle before calling cached()");
    }

    @Override
    public CompletionStage<Void> set(T model)
    {
        return client.set(model);
    }

    @Override
    public CompletionStage<Void> set(T model, int version)
    {
        return client.set(model, version);
    }

    @Override
    public CompletionStage<T> read()
    {
        return internalRead(Node::value);
    }

    @Override
    public CompletionStage<Node<T>> readAsNode()
    {
        return internalRead(Function.identity());
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

    @Override
    public CompletionStage<List<NodePath>> children()
    {
        return client.children();
    }

    @Override
    public CompletionStage<List<NodePath>> siblings()
    {
        return client.siblings();
    }

    private <U> CompletionStage<U> internalRead(Function<Node<T>, U> resolver)
    {
        NodePath path = client.modelSpec().path();
        Node<byte[]> data = cache.current().get(path);
        if ( data == null )
        {
            CompletableFuture<U> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpResponseException(HttpStatus.SC_NOT_FOUND, "Not Found"));
            return future;
        }
        return CompletableFuture.completedFuture(resolver.apply(ModeledHandleImpl.asNode(client.modelSpec(), data)));
    }
}
