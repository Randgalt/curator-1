package org.apache.curator.universal.consul.details;

import java.io.Closeable;
import java.net.URI;

class DeleteManager implements Closeable
{
    private final ConsulClientImpl client;

    DeleteManager(ConsulClientImpl client)
    {
        this.client = client;
    }

    void addDeletion(URI uri)
    {
        // TODO
    }

    @Override
    public void close()
    {

    }
}
