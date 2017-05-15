package org.apache.curator.universal.locks;

import java.util.concurrent.TimeUnit;

public interface CuratorLock
{
    boolean acquire(long time, TimeUnit unit);

    void release();
}
