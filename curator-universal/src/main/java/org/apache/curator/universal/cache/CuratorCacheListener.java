package org.apache.curator.universal.cache;

import org.apache.curator.universal.api.Node;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface CuratorCacheListener<T>
{
    enum Type
    {
        /**
         * A child was added to the path
         */
        NODE_ADDED,

        /**
         * A child's data was changed
         */
        NODE_UPDATED,

        /**
         * A child was removed from the path
         */
        NODE_REMOVED
    }

    /**
     * The given path was added, updated or removed
     *
     * @param type action type
     * @param node the node
     */
    void accept(Type type, Node<T> node);

    /**
     * The cache has finished initializing
     */
    default void initialized()
    {
        // NOP
    }

    /**
     * Called when there is an exception processing a message from the internal cache. This is most
     * likely due to a de-serialization problem.
     *
     * @param e the exception
     */
    default void handleException(Exception e)
    {
        LoggerFactory.getLogger(getClass()).error("Could not process cache message", e);
    }

    /**
     * Returns a version of this listener that only begins calling
     * {@link #accept(CuratorCacheListener.Type, org.apache.curator.universal.api.Node)}
     * once {@link #initialized()} has been called. i.e. changes that occur as the cache is initializing are not sent
     * to the listener
     *
     * @return wrapped listener
     */
    default CuratorCacheListener<T> postInitializedOnly()
    {
        return new CuratorCacheListener<T>()
        {
            private volatile boolean isInitialized = false;

            @Override
            public void accept(Type type, Node<T> node)
            {
                if ( isInitialized )
                {
                    CuratorCacheListener.this.accept(type, node);
                }
            }

            @Override
            public void initialized()
            {
                isInitialized = true;
                CuratorCacheListener.this.initialized();
            }
        };
    }
}
