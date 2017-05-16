package org.apache.curator.universal.listening;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Abstracts an object that has listeners
 */
public class ListenerContainer<T> implements Listenable<T>
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<T, ListenerEntry<T>> listeners = new ConcurrentHashMap<>();

    private static class ListenerEntry<T>
    {
        final T listener;
        final Executor executor;

        ListenerEntry(T listener, Executor executor)
        {
            this.listener = listener;
            this.executor = executor;
        }
    }

    @Override
    public void addListener(T listener)
    {
        addListener(listener, Runnable::run);
    }

    @Override
    public void addListener(T listener, Executor executor)
    {
        listeners.put(listener, new ListenerEntry<>(listener, executor));
    }

    @Override
    public void removeListener(T listener)
    {
        listeners.remove(listener);
    }

    /**
     * Remove all listeners
     */
    public void clear()
    {
        listeners.clear();
    }

    /**
     * Return the number of listeners
     *
     * @return number
     */
    public int size()
    {
        return listeners.size();
    }

    /**
     * Utility - apply the given function to each listener. The function receives
     * the listener as an argument.
     *
     * @param consumer function to call for each listener
     */
    public void forEach(final Consumer<T> consumer)
    {
        for ( final ListenerEntry<T> entry : listeners.values() )
        {
            entry.executor.execute(() -> {
                try
                {
                    consumer.accept(entry.listener);
                }
                catch ( Throwable e )
                {
                    if ( e instanceof InterruptedException )
                    {
                        Thread.currentThread().interrupt();
                    }
                    log.error(String.format("Listener (%s) threw an exception", entry.listener), e);
                }
            });
        }
    }
}
