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
package org.apache.curator.universal.consul.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.consul.details.ConsulClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

public interface ConsulClient extends Closeable
{
    static ConsulClientBuilder build(CloseableHttpAsyncClient client, URI baseUri)
    {
        return ConsulClientBuilder.build(client, baseUri);
    }

    void start();

    @Override
    void close();

    boolean blockUntilSession(Duration maxBlock);

    CompletionStage<JsonNode> read(NodePath path);

    CompletionStage<JsonNode> set(NodePath path, byte[] data);

    CompletionStage<JsonNode> set(NodePath path, int version, byte[] data);

    CompletionStage<JsonNode> delete(NodePath path);

    CompletionStage<JsonNode> delete(NodePath path, int version);
}
