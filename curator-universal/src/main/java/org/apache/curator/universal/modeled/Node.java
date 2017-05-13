package org.apache.curator.universal.modeled;

import org.apache.curator.universal.api.Metadata;
import org.apache.curator.universal.api.NodePath;

/**
 * Abstracts a ZooKeeper node
 */
public interface Node<T>
{
    /**
     * The path of the node
     *
     * @return path
     */
    NodePath path();

    Metadata metadata();

    /**
     * The node's current model
     *
     * @return model
     */
    T model();
}
