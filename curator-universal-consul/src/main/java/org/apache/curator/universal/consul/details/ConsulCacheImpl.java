package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.universal.api.NodePath;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ConsulCacheImpl implements Closeable
{
    private final ConsulClientImpl client;
    private final NodePath path;
    private final ExecutorService executor;
    private final AtomicReference<Future<?>> future = new AtomicReference<>(null);
    private final Map<NodePath, Entry> entries = new ConcurrentHashMap<>();
    private volatile int currentConsulIndex = -1;

    private static final Duration maxRead = Duration.ofMinutes(5);

    private static class Entry
    {
        private final byte[] data;
        private int index;

        Entry(byte[] data, int index)
        {
            this.data = data;
            this.index = index;
        }
    }

    public ConsulCacheImpl(ConsulClientImpl client, NodePath path)
    {
        this(client, path, Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("ConsulCacheImpl-%d").setDaemon(true).build()));
    }

    public ConsulCacheImpl(ConsulClientImpl client, NodePath path, ExecutorService executor)
    {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
    }

    public void start()
    {
        Future<?> future = executor.submit(this::processor);
        if ( !this.future.compareAndSet(null, future) )
        {
            future.cancel(true);
            throw new IllegalStateException("Already started");
        }
    }

    @Override
    public void close()
    {
        Future<?> future = this.future.getAndSet(null);
        if ( future != null )
        {
            future.cancel(true);
        }
    }

    private void processor()
    {
        while ( !Thread.currentThread().isInterrupted() )
        {
            process();
        }
    }

    private void process()
    {
        URI uri;
        if ( currentConsulIndex < 0 )
        {
            uri = client.buildUri(ApiPaths.keyValue, path.fullPath(), "recurse", true);
        }
        else
        {
            uri = client.buildUri(ApiPaths.keyValue, path.fullPath(), "index", currentConsulIndex, "recurse", true);
        }
        HttpGet request = new HttpGet(uri);
        try
        {
            Future<HttpResponse> future = client.httpClient().execute(request, null);
            SyncHandler handler = new SyncHandler(client.json(), future);
            SyncHandler.Response response = handler.get(maxRead.toMillis(), TimeUnit.MILLISECONDS, "Could not read path: ", path.toString());
            if ( response != null )
            {
                currentConsulIndex = response.consulIndex;
                processNodes(response.node);
            }
        }
        catch ( Exception e )
        {
            // TODO
        }
    }

    private void processNodes(JsonNode nodes)
    {
        Set<NodePath> usedSet = new HashSet<>(entries.keySet());
        for ( JsonNode node : nodes )
        {
            NodePath path = NodePath.parse(node.get("Key").asText());
            usedSet.remove(path);
            int index = node.get("ModifyIndex").asInt();
            Entry entry = entries.get(path);
            if ( (entry == null) || (entry.index != index) )
            {
                byte[] data = Base64.getDecoder().decode(node.get("Value").asText());
                entries.put(path, new Entry(data, index));
            }
        }

        usedSet.forEach(entries::remove);
    }
}
