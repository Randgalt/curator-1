package org.apache.curator.universal.consul.details;

import com.google.common.collect.ImmutableList;
import org.apache.curator.universal.consul.client.ConsulClient;
import org.apache.curator.universal.consul.retry.ExponentialBackoffRetry;
import org.apache.curator.universal.consul.retry.RetryPolicy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ConsulClientBuilder
{
    private final CloseableHttpAsyncClient client;
    private final URI baseUri;
    private String sessionName = "curator";
    private String ttl = "30s";
    private String lockDelay = "0s";
    private List<String> checks = Collections.emptyList();
    private Duration maxCloseSession = Duration.ofSeconds(5);
    private String authenticationToken = null;
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(Duration.ofSeconds(1), Duration.ofSeconds(30), 3);

    public static ConsulClientBuilder build(CloseableHttpAsyncClient client, URI baseUri)
    {
        return new ConsulClientBuilder(client, baseUri);
    }

    public ConsulClient build()
    {
        return new ConsulClientImpl(client, baseUri, sessionName, ttl, checks, lockDelay, maxCloseSession, authenticationToken, retryPolicy);
    }

    private ConsulClientBuilder(CloseableHttpAsyncClient client, URI baseUri)
    {
        this.client = client;
        this.baseUri = baseUri;
    }

    public ConsulClientBuilder sessionName(String sessionName)
    {
        this.sessionName = Objects.requireNonNull(sessionName, "sessionName cannot be null");
        return this;
    }

    public ConsulClientBuilder ttl(String ttl)
    {
        this.ttl = Objects.requireNonNull(ttl, "ttl cannot be null");
        return this;
    }

    public ConsulClientBuilder lockDelay(String lockDelay)
    {
        this.lockDelay = Objects.requireNonNull(lockDelay, "lockDelay cannot be null");
        return this;
    }

    public ConsulClientBuilder maxCloseSession(Duration maxCloseSession)
    {
        this.maxCloseSession = Objects.requireNonNull(maxCloseSession, "maxCloseSession cannot be null");
        return this;
    }

    public ConsulClientBuilder checks(List<String> checks)
    {
        this.checks = ImmutableList.copyOf(Objects.requireNonNull(checks, "checks cannot be null"));
        return this;
    }

    public ConsulClientBuilder authenticationToken(String authenticationToken)
    {
        this.authenticationToken = Objects.requireNonNull(authenticationToken, "authenticationToken cannot be null");
        return this;
    }

    public ConsulClientBuilder retryPolicy(RetryPolicy retryPolicy)
    {
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy cannot be null");
        return this;
    }
}
