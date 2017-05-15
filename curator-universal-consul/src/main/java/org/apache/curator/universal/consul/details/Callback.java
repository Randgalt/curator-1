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
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

class Callback implements FutureCallback<HttpResponse>
{
    private final CompletableFuture<JsonNode> future = new CompletableFuture<>();
    private final Json json;

    Callback(Json json)
    {
        this.json = json;
    }

    CompletableFuture<JsonNode> getFuture()
    {
        return future;
    }

    @Override
    public void completed(HttpResponse response)
    {
        try
        {
            JsonNode node = json.read(response.getEntity().getContent());
            future.complete(node);
        }
        catch ( Exception e )
        {
            failed(e);
        }
    }

    @Override
    public void failed(Exception ex)
    {
        future.completeExceptionally(ex);
    }

    @Override
    public void cancelled()
    {
        future.completeExceptionally(new CancellationException());
    }
}
