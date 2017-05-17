package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.universal.api.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Session implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConsulClientImpl client;
    private final String sessionName;
    private final String ttlString;
    private final List<String> checks;
    private final String lockDelay;
    private final Duration maxCloseSession;
    private final ScheduledExecutorService executorService;
    private final Duration sessionLength;

    private volatile Duration ttl;
    private volatile String sessionId = errorSessionId;
    private volatile Instant startOfSuspended = null;

    private static final String errorSessionId = "";

    Session(ConsulClientImpl client, String sessionName, String ttl, List<String> checks, String lockDelay, Duration maxCloseSession)
    {
        this.client = client;
        this.sessionName = sessionName;
        this.ttlString = ttl;
        this.checks = checks;
        this.lockDelay = lockDelay;
        this.maxCloseSession = maxCloseSession;
        this.ttl = this.sessionLength = TimeStrings.parse(ttl);

        executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Session-%d").setDaemon(true).build());
    }

    void start()
    {
        executorService.schedule(this::checkSession, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close()
    {
        executorService.shutdownNow();

        String localSessionId = sessionId;
        sessionId = errorSessionId;

        if ( !localSessionId.equals(errorSessionId) )
        {
            URI uri = client.buildUri(ApiPaths.deleteSession, localSessionId);
            CompletableFuture<ApiRequest.Response> future = client.newApiRequest().delete(uri);
            ApiRequest.get(future, maxCloseSession.toMillis(), TimeUnit.MILLISECONDS, "Could not delete session: ", localSessionId);
        }
    }

    String sessionId()
    {
        return sessionId;
    }

    private void checkSession()
    {
        String localSessionId = sessionId;
        try
        {
            if ( localSessionId.equals(errorSessionId) )
            {
                createSession();
            }
            else
            {
                renewSession();
            }
        }
        finally
        {
            Duration adjusted = ttl.multipliedBy(2).dividedBy(3);   // 2/3 of the ttl
            executorService.schedule(this::checkSession, adjusted.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void createSession()
    {
        ObjectNode node = client.json().mapper().createObjectNode();
        node.put("Name", sessionName);
        node.put("TTL", ttlString);
        if ( checks.size() > 0 )
        {
            ArrayNode tab = client.json().mapper().createArrayNode();
            checks.forEach(tab::add);
            node.set("Checks", tab);
        }
        if ( !lockDelay.isEmpty() )
        {
            node.put("LockDelay", lockDelay);
        }
        URI uri = client.buildUri(ApiPaths.createSession, null);
        try
        {
            CompletableFuture<ApiRequest.Response> future = client.newApiRequest().put(uri, client.json().mapper().writeValueAsBytes(node));
            future.whenComplete((response, e) -> {
                if ( e != null )
                {
                    log.error("Session creation failed", e);
                    setSessionId(errorSessionId);
                }
                else
                {
                    setSessionId(response.node.get("ID").asText());
                }
            });
            future.exceptionally(e -> {
                log.error("Could not create session", e);
                setSessionId(errorSessionId);
                return null;
            });
        }
        catch ( JsonProcessingException e )
        {
            throw new RuntimeException("Could not serialize session", e);
        }
    }

    private void renewSession()
    {
        String localSessionId = sessionId;
        if ( !localSessionId.equals(errorSessionId) )
        {
            URI uri = client.buildUri(ApiPaths.renewSession, localSessionId);
            CompletableFuture<ApiRequest.Response> future = client.newApiRequest().put(uri);
            future.thenAccept(response -> {
                if ( response.node.has("TTL") )
                {
                    String ttlString = response.node.get("TTL").asText();
                    try
                    {
                        Duration newTtl = Duration.parse(ttlString);
                        if ( !newTtl.equals(ttl) )
                        {
                            log.info("Server is changing the TTL to: " + ttl);
                            ttl = newTtl;
                        }
                    }
                    catch ( Exception e )
                    {
                        log.error("Could not parse ttl string from server: " + ttlString, e);
                    }
                }
                setSessionId(localSessionId);
            });
            future.exceptionally(e -> {
                log.error("Could not renew session: " + localSessionId, e);
                setSessionId(errorSessionId);
                return null;
            });
        }
    }

    private synchronized void setSessionId(String newId)
    {
        sessionId = newId;
        boolean isErrorId = sessionId.equals(errorSessionId);

        SessionState newState;
        if ( isErrorId )
        {
            newState = SessionState.SUSPENDED;
            if ( startOfSuspended == null )
            {
                startOfSuspended = Instant.now();
            }
            else
            {
                Duration elapsed = Duration.between(startOfSuspended, Instant.now());
                if ( elapsed.compareTo(sessionLength) >= 0 )
                {
                    newState = SessionState.LOST;
                }
            }
        }
        else
        {
            startOfSuspended = null;
            newState = SessionState.CONNECTED;
        }

        client.updateSessionState(newState);
    }
}
