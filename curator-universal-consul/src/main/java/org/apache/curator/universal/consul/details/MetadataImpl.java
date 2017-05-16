package org.apache.curator.universal.consul.details;

import org.apache.curator.universal.api.Metadata;

class MetadataImpl implements Metadata
{
    private final int index;

    MetadataImpl(int index)
    {
        this.index = index;
    }

    @Override
    public int version()
    {
        return index;
    }

    @Override
    public int transaction()
    {
        return index;
    }
}
