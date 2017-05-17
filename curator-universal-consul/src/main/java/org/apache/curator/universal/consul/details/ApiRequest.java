package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.universal.consul.retry.RetryPolicy;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.nio.client.HttpAsyncClient;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

abstract class ApiRequest
{
    private boolean useRetryPolicy = true;

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
        private final Supplier<HttpRequestBase> requestSupplier;
        private final RetryPolicy retryPolicy;
        private final Json json;
        private final AtomicInteger retryCount = new AtomicInteger(0);
        private final Instant requestStart = Instant.now();

        private Callback(Json json, CompletableFuture<Response> future, Supplier<HttpRequestBase> requestSupplier, RetryPolicy retryPolicy)
        {
            this.json = json;
            this.future = future;
            this.requestSupplier = requestSupplier;
            this.retryPolicy = retryPolicy;
        }

        @Override
        public void completed(HttpResponse response)
        {
            int statusFamily = response.getStatusLine().getStatusCode() / 100;
            if ( response.getStatusLine().getStatusCode() != 2 )
            {
                HttpResponseException exception = new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                boolean canRetry = (response.getStatusLine().getStatusCode() == 5);
                failOrRetry(canRetry, exception);
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
        public void failed(Exception exception)
        {
            failOrRetry(exceptionCanBeRetried(exception), exception);
        }

        @Override
        public void cancelled()
        {
            future.completeExceptionally(new CancellationException());
        }

        private void failOrRetry(boolean canRetry, Exception exception)
        {
            if ( canRetry && retryPolicy.allowRetry(retryCount.getAndIncrement(), Duration.between(requestStart, Instant.now())) )
            {
                internalRequest(requestSupplier, this);
            }
            else
            {
                future.completeExceptionally(exception);
            }
        }
    }

    CompletableFuture<Response> get(URI uri)
    {
        return internalRequest(() -> new HttpGet(uri));
    }

    CompletableFuture<Response> put(URI uri)
    {
        return internalRequest(() -> new HttpPut(uri));
    }

    CompletableFuture<Response> put(URI uri, byte[] data)
    {
        return internalRequest(() -> {
            HttpPut request = new HttpPut(uri);
            request.setEntity(new ByteArrayEntity(data));
            return request;
        });
    }

    CompletableFuture<Response> delete(URI uri)
    {
        return internalRequest(() -> new HttpDelete(uri));
    }

    ApiRequest useRetryPolicy(boolean useRetryPolicy)
    {
        this.useRetryPolicy = useRetryPolicy;
        return this;
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

    private CompletableFuture<Response> internalRequest(Supplier<HttpRequestBase> requestSupplier)
    {
        RetryPolicy retryPolicy = useRetryPolicy ? getRetryPolicy() : RetryPolicy.none();
        CompletableFuture<Response> future = new CompletableFuture<>();
        Callback callback = new Callback(json(), future, requestSupplier, retryPolicy);
        internalRequest(requestSupplier, callback);
        return future;
    }

    private void internalRequest(Supplier<HttpRequestBase> requestSupplier, Callback callback)
    {
        HttpRequestBase request = requestSupplier.get();
        String authenticationToken = getAuthenticationToken();
        if ( authenticationToken != null )
        {
            request.setHeader("X-Consul-Token", authenticationToken);
        }
        httpClient().execute(request, callback);
    }

    protected abstract RetryPolicy getRetryPolicy();

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

    private boolean exceptionCanBeRetried(Exception e)
    {
        return (e instanceof ConnectionClosedException) ||
            (e instanceof ConnectException) ||
            (e instanceof InterruptedIOException)
            ;
    }
}
