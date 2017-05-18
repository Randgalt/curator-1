package org.apache.curator.universal.consul.retry;

import java.time.Duration;
import java.util.Objects;

public class StandardRetry implements RetryPolicy
{
    private final Duration sleepTime;
    private final int maxRetries;

    public StandardRetry(Duration sleepTime, int maxRetries)
    {
        this.sleepTime = Objects.requireNonNull(sleepTime, "sleepTime cannot be null");
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean allowRetry(int retryCount, Duration elapsed)
    {
        return (retryCount < maxRetries) && sleep(sleepTime);
    }
}
