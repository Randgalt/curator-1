package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.nio.client.HttpAsyncClient;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class ApiRequest
{
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

    private class Callback implements FutureCallback<HttpResponse>
    {
        private final CompletableFuture<Response> future;
        private final Json json;

        private Callback(Json json, CompletableFuture<Response> future)
        {
            this.json = json;
            this.future = future;
        }

        @Override
        public void completed(HttpResponse response)
        {
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK )
            {
                failed(new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
            }
            else
            {
                try
                {
                    JsonNode node = json.read(response.getEntity().getContent());
                    future.complete(new Response(node, getConsulIndex(response)));
                }
                catch ( Exception e )
                {
                    failed(e);
                }
            }
        }

        @Override
        public void failed(Exception ex)
        {
            future.completeExceptionally(ex);
        }

        @Override
        public void cancelled()
        {
            future.completeExceptionally(new CancellationException());
        }
    }

    CompletableFuture<Response> get(URI uri)
    {
        return internalRequest(new HttpGet(uri));
    }

    CompletableFuture<Response> put(URI uri)
    {
        return internalRequest(new HttpPut(uri));
    }

    CompletableFuture<Response> put(URI uri, byte[] data)
    {
        HttpPut request = new HttpPut(uri);
        request.setEntity(new ByteArrayEntity(data));
        return internalRequest(request);
    }

    CompletableFuture<Response> delete(URI uri)
    {
        return internalRequest(new HttpDelete(uri));
    }

    static Response get(CompletableFuture<Response> future, long time, TimeUnit unit, String errorMessage, Object errorMessageArgument)
    {
        try
        {
            return future.get(time, unit);
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
        catch ( ExecutionException e )
        {
            throw new RuntimeException(errorMessage + errorMessageArgument, e);
        }
        catch ( TimeoutException ignore )
        {
            // ignore
        }
        return null;
    }

    private CompletableFuture<Response> internalRequest(HttpRequestBase request)
    {
        String authenticationToken = getAuthenticationToken();
        if ( authenticationToken != null )
        {
            request.setHeader("X-Consul-Token", authenticationToken);
        }
        CompletableFuture<Response> future = new CompletableFuture<>();
        Callback callback = new Callback(json(), future);
        httpClient().execute(request, callback);
        return future;
    }

    protected abstract String getAuthenticationToken();

    protected abstract Json json();

    protected abstract HttpAsyncClient httpClient();

    private static int getConsulIndex(HttpResponse response)
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
