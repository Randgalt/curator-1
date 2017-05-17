package org.apache.curator.universal.consul.retry;

import java.time.Duration;
import java.util.Objects;

public class RetryForever implements RetryPolicy
{
    private final Duration sleepTime;

    public RetryForever(Duration sleepTime)
    {
        this.sleepTime = Objects.requireNonNull(sleepTime, "sleepTime cannot be null");
    }

    @Override
    public boolean allowRetry(int retryCount, Duration elapsed)
    {
        try
        {
            sleep(sleepTime);
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }
}
