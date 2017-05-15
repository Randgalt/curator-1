package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.locks.CuratorLock;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Future;
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

        boolean result = false;
        int consulIndex = -1;
        while ( remainingNanos > 0 )
        {
            long startNanos = System.nanoTime();

            HttpPut put = client.putRequest(makeUri(), new byte[0]);
            Future<HttpResponse> future = client.httpClient().execute(put, null);
            SyncHandler handler = new SyncHandler(client.json(), future);
            SyncHandler.Response response = handler.get(remainingNanos, TimeUnit.NANOSECONDS, "Acquiring lock: ", lockPath);
            if ( response == null )
            {
                break;
            }
            if ( response.node.asBoolean() )
            {
                result = true;
                break;
            }

            URI uri;
            if ( consulIndex >= 0 )
            {
                uri = client.buildUri(ApiPaths.keyValue, lockPath.fullPath(), "index", consulIndex, "wait", TimeStrings.toSeconds(Duration.ofNanos(remainingNanos)));
            }
            else
            {
                uri = client.buildUri(ApiPaths.keyValue, lockPath.fullPath());
            }
            HttpGet get = new HttpGet(uri);
            future = client.httpClient().execute(get, null);
            handler = new SyncHandler(client.json(), future);
            response = handler.get(remainingNanos, TimeUnit.NANOSECONDS, "Acquiring lock: ", lockPath);
            if ( response == null )
            {
                break;
            }

            JsonNode firstChild = Json.requireFirstChild(response.node);
            if ( firstChild.get("Session").asText().equals(client.sessionId()) )
            {
                break;
            }
            consulIndex = response.consulIndex;

            long elapsedNanos = System.nanoTime() - startNanos;
            remainingNanos -= elapsedNanos;
        }
        return result;
    }

    @Override
    public void release()
    {

    }

    private URI makeUri()
    {
        // TODO - handle no session
        return client.buildUri(ApiPaths.keyValue, lockPath.fullPath(), "acquire", client.sessionId());
    }
}
