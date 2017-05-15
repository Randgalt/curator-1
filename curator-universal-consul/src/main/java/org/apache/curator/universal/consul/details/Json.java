package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class Json
{
    private final ObjectMapper mapper;

    Json()
    {
        mapper = new ObjectMapper();    // TODO
    }

    static JsonNode requireFirstChild(JsonNode node)
    {
        if ( !node.isArray() )
        {
            throw new IllegalStateException("Expecting an array of values");
        }
        if ( node.size() == 0 )
        {
            throw new IndexOutOfBoundsException("No values returned");
        }
        if ( node.size() > 1 )
        {
            throw new IndexOutOfBoundsException("Multiple values returned: " + node.size());
        }
        return node.get(0);
    }

    ObjectMapper mapper()
    {
        return mapper;
    }

    JsonNode read(InputStream stream)
    {
        try
        {
            return read(CharStreams.toString(new InputStreamReader(stream)));
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Could not read json stream", e);
        }
    }

    JsonNode read(String content)
    {
        try
        {
            return mapper.readTree(content.trim());
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Could not process json content: " + content, e);
        }
    }
}
