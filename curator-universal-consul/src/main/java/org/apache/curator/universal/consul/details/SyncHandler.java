package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

        Response(JsonNode node, int consulIndex)
        {
            this.node = node;
            this.consulIndex = consulIndex;
        }
    }

    Response get(long time, TimeUnit unit, String errorMessage, Object errorMessageArgument)
    {
        try
        {
            HttpResponse response = future.get(time, unit);
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK )
            {
                throw new RuntimeException(new HttpException(response.getStatusLine().toString()));
            }

            return new Response(
                json.read(response.getEntity().getContent()),
                getConsulIndex(response));
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
