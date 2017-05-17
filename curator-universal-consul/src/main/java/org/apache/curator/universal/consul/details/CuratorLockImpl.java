package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.locks.CuratorLock;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class CuratorLockImpl implements CuratorLock
{
    private final ConsulClientImpl client;
    private final NodePath lockPath;

    CuratorLockImpl(ConsulClientImpl client, NodePath lockPath)
    {
        this.client = client;
        this.lockPath = lockPath;
    }

    @Override
    public boolean acquire(long time, TimeUnit unit)
    {
        // TODO payload

        long remainingNanos = unit.toNanos(time);

        boolean hasTheLock = false;
        int consulIndex = -1;
        while ( remainingNanos > 0 )
        {
            long startNanos = System.nanoTime();

            CompletableFuture<ApiRequest.Response> future = client.newApiRequest().put(makeUri("acquire"));
            ApiRequest.Response response = ApiRequest.get(future, remainingNanos, TimeUnit.NANOSECONDS, "Acquiring lock: ", lockPath);
            if ( response == null )
            {
                break;
            }
            if ( response.node.asBoolean() )
            {
                hasTheLock = true;
                break;
            }

            URI uri;
            if ( consulIndex >= 0 )
            {
                // TODO include lock delay
                uri = client.buildUri(ApiPaths.keyValue, lockPath.fullPath(), "index", consulIndex, "wait", TimeStrings.toSeconds(Duration.ofNanos(remainingNanos)));
            }
            else
            {
                uri = client.buildUri(ApiPaths.keyValue, lockPath.fullPath());
            }
            future = client.newApiRequest().get(uri);
            response = ApiRequest.get(future, remainingNanos, TimeUnit.NANOSECONDS, "Acquiring lock: ", lockPath);
            if ( response == null )
            {
                break;
            }

            JsonNode firstChild = Json.requireFirstChild(response.node);
            if ( firstChild.has("Session") && firstChild.get("Session").asText().equals(client.sessionId()) )
            {
                break;
            }
            consulIndex = response.consulIndex;

            long elapsedNanos = System.nanoTime() - startNanos;
            remainingNanos -= elapsedNanos;
        }
        return hasTheLock;
    }

    @Override
    public void release()
    {
        // TODO use Delete Manager
        client.newApiRequest().put(makeUri("release"));
    }

    private URI makeUri(String mode)
    {
        // TODO - handle no session
        return client.buildUri(ApiPaths.keyValue, lockPath.fullPath(), mode, client.sessionId());
    }
}
