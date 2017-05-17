package org.apache.curator.universal.consul.retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface RetryPolicy
{
    /**
     * Called when an operation has failed for some reason. This method should return
     * true to make another attempt.
     *
     *
     * @param retryCount the number of times retried so far (0 the first time)
     * @param elapsed the elapsed time in since the operation was attempted
     * @return true/false
     */
    boolean      allowRetry(int retryCount, Duration elapsed);

    default void sleep(Duration time) throws InterruptedException
    {
        TimeUnit.NANOSECONDS.sleep(time.toNanos());
    }

    static RetryPolicy none()
    {
        return (retryCount, elapsed) -> false;
    }
}
