/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.curator.universal.consul.details;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.consul.client.ConsulClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ConsulClientImpl implements ConsulClient
{
    private final CloseableHttpAsyncClient client;
    private final URI baseUri;
    private final ObjectMapper mapper;

    public ConsulClientImpl(URI baseUri)
    {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri cannot be null");
        client = HttpAsyncClients.createDefault();  // lots TODO

        mapper = new ObjectMapper();    // TODO - proper config
    }

    @Override
    public void start()
    {
        client.start();
    }

    @Override
    public void close()
    {
        try
        {
            client.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Could not close http client", e);
        }
    }

    @Override
    public CompletionStage<JsonNode> read(NodePath path)
    {
        return request(path, HttpGet::new);
    }

    @Override
    public CompletionStage<JsonNode> set(NodePath path, int version, byte[] data)
    {
        return request(path, version, uri -> putRequest(uri, data));
    }

    @Override
    public CompletionStage<JsonNode> set(NodePath path, byte[] data)
    {
        return request(path, uri -> putRequest(uri, data));
    }

    @Override
    public CompletionStage<JsonNode> delete(NodePath path)
    {
        return request(path, HttpDelete::new);
    }

    @Override
    public CompletionStage<JsonNode> delete(NodePath path, int version)
    {
        return request(path, version, HttpDelete::new);
    }

    private HttpPut putRequest(URI uri, byte[] data)
    {
        HttpPut request = new HttpPut(uri);
        request.setEntity(new ByteArrayEntity(data));
        return request;
    }

    private CompletionStage<JsonNode> request(NodePath path, Function<URI, HttpRequestBase> builder)
    {
        return request(path, -1, builder);
    }

    private CompletionStage<JsonNode> request(NodePath path, int version, Function<URI, HttpRequestBase> builder)
    {
        Callback callback = new Callback(mapper);
        HttpRequestBase request = builder.apply(buildUri("/v1/kv/", path.fullPath(), version));
        client.execute(request, callback);
        return callback.getFuture();
    }

    private URI buildUri(String apiPath, String extra, int version)
    {
        String path = apiPath;
        if ( extra != null )
        {
            if ( !path.endsWith("/") )
            {
                path += "/";
            }
            try
            {
                path += URLEncoder.encode(extra, "UTF-8");
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new RuntimeException(e);
            }
        }

        try
        {
            URIBuilder builder = new URIBuilder(baseUri).setPath(path);
            if ( version >= 0 )
            {
                builder = builder.addParameter("cas", Integer.toString(version));
            }
            return builder.build();
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException(e);
        }
    }

}
