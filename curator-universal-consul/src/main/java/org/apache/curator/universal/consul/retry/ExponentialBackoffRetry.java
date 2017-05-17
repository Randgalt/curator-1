package org.apache.curator.universal.consul.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ExponentialBackoffRetry implements RetryPolicy
{
    private final Duration baseSleepTime;
    private final Duration maxSleepTime;
    private final int maxRetries;

    public ExponentialBackoffRetry(Duration baseSleepTime, Duration maxSleepTime, int maxRetries)
    {
        this.baseSleepTime = Objects.requireNonNull(baseSleepTime, "baseSleepTime cannot be null");
        this.maxSleepTime = Objects.requireNonNull(maxSleepTime, "maxSleepTime cannot be null");
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean allowRetry(int retryCount, Duration elapsed)
    {
        if ( retryCount < maxRetries )
        {
            // copied from Hadoop's RetryPolicies.java
            Duration exponentialSleep = baseSleepTime.multipliedBy(Math.max(1, ThreadLocalRandom.current().nextInt(1 << (retryCount + 1))));
            Duration thisSleep = (exponentialSleep.compareTo(maxSleepTime) > 0) ? maxSleepTime : exponentialSleep;

            try
            {
                sleep(thisSleep);
            }
            catch ( InterruptedException e )
            {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
        return false;
    }
}
