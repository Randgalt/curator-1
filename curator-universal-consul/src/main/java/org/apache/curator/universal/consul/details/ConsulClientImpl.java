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
import org.apache.curator.universal.api.CuratorHandle;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.api.SessionState;
import org.apache.curator.universal.api.SessionStateListener;
import org.apache.curator.universal.consul.client.ConsulClient;
import org.apache.curator.universal.listening.Listenable;
import org.apache.curator.universal.listening.ListenerContainer;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class ConsulClientImpl implements ConsulClient
{
    private final CloseableHttpAsyncClient client;
    private final String authenticationToken;
    private final URI baseUri;
    private final Json json = new Json();
    private final Session session;
    private final DeleteManager deleteManager;
    private final AtomicReference<SessionState> sessionState = new AtomicReference<>(SessionState.LATENT);
    private final ListenerContainer<SessionStateListener> sessionStateListeners = new ListenerContainer<>();

    ConsulClientImpl(CloseableHttpAsyncClient client, URI baseUri, String sessionName, String ttl, List<String> checks, String lockDelay, Duration maxCloseSession, String authenticationToken)
    {
        this.baseUri = baseUri;
        this.client = client;
        this.authenticationToken = authenticationToken;
        session = new Session(this, sessionName, ttl, checks, lockDelay, maxCloseSession);
        deleteManager = new DeleteManager(this);
    }

    @Override
    public CuratorHandle asCuratorHandle()
    {
        return new ConsulCuratorHandle(this);
    }

    @Override
    public void start()
    {
        client.start();
        session.start();
    }

    @Override
    public void close()
    {
        deleteManager.close();
        session.close();
        try
        {
            client.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Could not close http client", e);
        }
    }

    @Override
    public Listenable<SessionStateListener> sessionStateListenable()
    {
        return sessionStateListeners;
    }

    @Override
    public boolean blockUntilSession(Duration maxBlock)
    {
        Semaphore semaphore = new Semaphore(0);
        SessionStateListener listener = __ -> semaphore.release();
        sessionStateListeners.addListener(listener);
        try
        {
            while ( (maxBlock.toNanos() > 0) && (sessionState.get() != SessionState.CONNECTED) )
            {
                long startNanos = System.nanoTime();
                try
                {
                    semaphore.tryAcquire(maxBlock.toNanos(), TimeUnit.NANOSECONDS);
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    return false;
                }
                long elapsed = System.nanoTime() - startNanos;
                maxBlock = maxBlock.minusNanos(elapsed);
            }
        }
        finally
        {
            sessionStateListeners.removeListener(listener);
        }

        return sessionState.get() == SessionState.CONNECTED;
    }

    @Override
    public SessionState sessionState()
    {
        return sessionState.get();
    }

    @Override
    public CompletionStage<JsonNode> read(NodePath path)
    {
        return newApiRequest().get(buildUri(ApiPaths.keyValue, path.fullPath())).thenApply(response -> response.node);
    }

    @Override
    public CompletionStage<JsonNode> set(NodePath path, int version, byte[] data)
    {
        return newApiRequest().put(buildVersionedUri(ApiPaths.keyValue, path.fullPath(), version), data).thenApply(response -> response.node);
    }

    @Override
    public CompletionStage<JsonNode> set(NodePath path, byte[] data)
    {
        return newApiRequest().put(buildUri(ApiPaths.keyValue, path.fullPath())).thenApply(response -> response.node);
    }

    @Override
    public CompletionStage<JsonNode> delete(NodePath path)
    {
        return newApiRequest().delete(buildUri(ApiPaths.keyValue, path.fullPath())).thenApply(response -> response.node);
    }

    @Override
    public CompletionStage<JsonNode> delete(NodePath path, int version)
    {
        return newApiRequest().delete(buildVersionedUri(ApiPaths.keyValue, path.fullPath(), version)).thenApply(response -> response.node);
    }

    @Override
    public CompletionStage<List<NodePath>> children(NodePath path)
    {
        CompletableFuture<ApiRequest.Response> future = newApiRequest().get(buildUri(ApiPaths.keyValue, path.fullPath(), "recurse", true, "keys", true));
        return future.thenApply(response -> {
            List<NodePath> paths = new ArrayList<>();
            for ( JsonNode child : response.node )
            {
                NodePath childPath = NodePath.parse(child.asText());
                if ( childPath.parent().equals(path) )
                {
                    paths.add(childPath);
                }
            }
            return paths;
        });
    }

    HttpPut putRequest(URI uri, byte[] data)
    {
        HttpPut request = new HttpPut(uri);
        request.setEntity(new ByteArrayEntity(data));
        return request;
    }

    ApiRequest newApiRequest()
    {
        return new ApiRequest()
        {
            @Override
            protected Json json()
            {
                return json;
            }

            @Override
            protected HttpAsyncClient httpClient()
            {
                return client;
            }

            @Override
            protected String getAuthenticationToken()
            {
                return authenticationToken;
            }
        };
    }

    Json json()
    {
        return json;
    }

    URI buildUri(String apiPath, String extra, Object... queryParameters)
    {
        String path = apiPath;
        if ( extra != null )
        {
            if ( extra.startsWith("/") )
            {
                path += extra;
            }
            else
            {
                path += "/" + extra;
            }
        }

        try
        {
            URIBuilder builder = new URIBuilder(baseUri).setPath(path);
            if ( queryParameters != null )
            {
                for ( int i = 0; (i + 1) < queryParameters.length; i += 2 )
                {
                    builder = builder.addParameter(String.valueOf(queryParameters[i]), String.valueOf(queryParameters[i + 1]));
                }
            }
            return builder.build();
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException(e);
        }
    }

    String sessionId()
    {
        return session.sessionId();
    }

    void updateSessionState(SessionState newState)
    {
        SessionState oldState = sessionState.getAndSet(newState);
        if ( oldState != newState )
        {
            sessionStateListeners.forEach(l -> l.newSessionState(newState));
        }
    }

    private URI buildVersionedUri(String apiPath, String extra, int version)
    {
        return (version >= 0) ? buildUri(apiPath, extra, "cas", version) : buildUri(apiPath, extra);
    }
}
