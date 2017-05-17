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
package org.apache.curator.universal.modeled;

import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.cache.CuratorCacheListener;
import org.apache.curator.universal.listening.Listenable;
import java.io.Closeable;
import java.util.concurrent.Executor;

public interface CachedModeledHandle<T> extends ModeledHandle<T>, Closeable
{
    /**
     * Start the internally created cache
     */
    void start();

    /**
     * Close/stop the internally created cache
     */
    @Override
    void close();

    Executor executor();

    /**
     * Return the listener container so that you can add/remove listeners
     *
     * @return listener container
     */
    Listenable<CuratorCacheListener<T>> listenable();

    /**
     * {@inheritDoc}
     */
    @Override
    CachedModeledHandle<T> child(Object child);

    /**
     * {@inheritDoc}
     */
    @Override
    CachedModeledHandle<T> withPath(NodePath path);
}
