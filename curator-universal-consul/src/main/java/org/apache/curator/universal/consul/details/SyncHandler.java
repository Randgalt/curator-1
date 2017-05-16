package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class SyncHandler
{
    private final Json json;
    private final Future<HttpResponse> future;

    SyncHandler(Json json, Future<HttpResponse> future)
    {
        this.json = json;
        this.future = future;
    }

    static class Response
    {
        final JsonNode node;
        final int consulIndex;
        final int status;

        Response(JsonNode node, int consulIndex, int status)
        {
            this.node = node;
            this.consulIndex = consulIndex;
            this.status = status;
        }
    }

    Response get(long time, TimeUnit unit, String errorMessage, Object errorMessageArgument)
    {
        try
        {
            HttpResponse response = future.get(time, unit);
            return new Response(
                json.read(response.getEntity().getContent()),
                getConsulIndex(response),
                response.getStatusLine().getStatusCode()
            );
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
        catch ( ExecutionException | IOException e )
        {
            throw new RuntimeException(errorMessage + errorMessageArgument, e);
        }
        catch ( TimeoutException ignore )
        {
            // ignore
        }
        return null;
    }

    static int getConsulIndex(HttpResponse response)
    {
        Header header = response.getFirstHeader("X-Consul-Index");
        try
        {
            return (header != null) ? Integer.parseInt(header.getValue()) : -1;
        }
        catch ( NumberFormatException ignore )
        {
            // ignore
        }
        return -1;
    }
}
