package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.universal.api.Metadata;

class MetadataImpl implements Metadata
{
    MetadataImpl(JsonNode node)
    {
        // TODO
    }

    @Override
    public int version()
    {
        return 0;
    }

    @Override
    public int transaction()
    {
        return 0;
    }
}
