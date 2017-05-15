package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final CountDownLatch sessionSetLatch = new CountDownLatch(1);
    private final ScheduledExecutorService executorService;
    private volatile Duration ttl;
    private volatile String sessionId = errorSessionId;

    private static final String errorSessionId = "";

    Session(ConsulClientImpl client, String sessionName, String ttl, List<String> checks, String lockDelay, Duration maxCloseSession)
    {
        this.client = client;
        this.sessionName = sessionName;
        this.ttlString = ttl;
        this.checks = checks;
        this.lockDelay = lockDelay;
        this.maxCloseSession = maxCloseSession;
        this.ttl = TimeStrings.parse(ttl);

        executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Session-%d").setDaemon(true).build());
    }

    void start()
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
            HttpPut request = client.putRequest(uri, client.json().mapper().writeValueAsBytes(node));
            Callback callback = new Callback(client.json());
            client.httpClient().execute(request, callback);
            callback.getFuture().whenComplete((result, e) -> {
                if ( e != null )
                {
                    log.error("Session creation failed", e);
                    sessionId = errorSessionId;
                }
                else
                {
                    sessionId = result.get("ID").asText();
                }
                sessionSetLatch.countDown();

                scheduleNextRenewal();
            });
        }
        catch ( JsonProcessingException e )
        {
            throw new RuntimeException("Could not serialize session", e);
        }
    }

    @Override
    public void close()
    {
        executorService.shutdownNow();

        String localSessionId = sessionId;
        sessionId = errorSessionId; // small race can occur at these two lines but it's not a big deal

        if ( !localSessionId.equals(errorSessionId) )
        {
            URI uri = client.buildUri(ApiPaths.deleteSession, localSessionId);
            HttpDelete request = new HttpDelete(uri);
            Future<HttpResponse> future = client.httpClient().execute(request, null);
            try
            {
                future.get(maxCloseSession.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch ( InterruptedException dummy )
            {
                Thread.interrupted();
            }
            catch ( Exception e )
            {
                log.error("Could not delete session: " + localSessionId, e);
            }
        }
    }

    String sessionId()
    {
        return sessionId;
    }

    CountDownLatch sessionSetLatch()
    {
        return sessionSetLatch;
    }

    private void renewSession()
    {
        try
        {
            String localSessionId = sessionId;
            if ( !localSessionId.equals(errorSessionId) )
            {
                URI uri = client.buildUri(ApiPaths.renewSession, localSessionId);
                HttpPut request = new HttpPut(uri);
                Callback callback = new Callback(client.json());
                client.httpClient().execute(request, callback);
                callback.getFuture().thenAccept(node -> {
                    if ( node.has("TTL") )
                    {
                        String ttlString = node.get("TTL").asText();
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
                });
            }
        }
        finally
        {
            scheduleNextRenewal();
        }
    }

    private void scheduleNextRenewal()
    {
        Duration adjusted = ttl.multipliedBy(2).dividedBy(3);   // 2/3 of the ttl
        executorService.schedule(this::renewSession, adjusted.toMillis(), TimeUnit.MILLISECONDS);
    }
}
