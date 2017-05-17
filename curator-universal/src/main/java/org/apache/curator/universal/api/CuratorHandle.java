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
package org.apache.curator.universal.api;

import org.apache.curator.universal.cache.CuratorCache;
import org.apache.curator.universal.listening.Listenable;
import org.apache.curator.universal.locks.CuratorLock;
import org.apache.curator.universal.modeled.ModelSpec;
import org.apache.curator.universal.modeled.ModeledHandle;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface CuratorHandle
{
    <T> ModeledHandle<T> wrap(ModelSpec<T> modelSpec);

    CuratorLock createLock(NodePath lockPath);

    SessionState sessionState();

    boolean blockUntilSession(Duration maxBlock);

    Listenable<SessionStateListener> sessionStateListenable();

    CuratorCache newCuratorCache(NodePath path);

    CompletionStage<Node<byte[]>> read(NodePath path);

    CompletionStage<Void> set(NodePath path, byte[] data);

    CompletionStage<Void> set(NodePath path, int version, byte[] data);

    CompletionStage<Void> delete(NodePath path);

    CompletionStage<Void> delete(NodePath path, int version);

    /**
     * Return the child paths of the given path (in no particular order)
     *
     * @param path parent path
     * @return AsyncStage
     */
    CompletionStage<List<NodePath>> children(NodePath path);
}
