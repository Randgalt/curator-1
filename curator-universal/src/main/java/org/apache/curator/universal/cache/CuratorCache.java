package org.apache.curator.universal.cache;

import org.apache.curator.universal.api.Node;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.listening.Listenable;
import java.io.Closeable;
import java.util.Map;

public interface CuratorCache extends Closeable
{
    void start();

    @Override
    void close();

    Map<NodePath, Node<byte[]>> current();

    Listenable<CuratorCacheListener<byte[]>> listenable();
}
