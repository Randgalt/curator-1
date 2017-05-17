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

import org.apache.curator.universal.api.Node;
import org.apache.curator.universal.api.NodePath;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public interface ModeledHandle<T>
{
    /**
     * <p>
     *     Use an internally created cache as a front for this modeled instance. All read APIs use the internal
     *     cache. i.e. read calls always use the cache instead of making direct queries. Note: you must call
     *     {@link org.apache.curator.universal.modeled.CachedModeledHandle#start()} and
     *     {@link org.apache.curator.universal.modeled.CachedModeledHandle#close()} to start/stop
     * </p>
     *
     * <p>
     *     Note: this method internally allocates an Executor for the cache and read methods. Use
     *     {@link #cached(java.util.concurrent.ExecutorService)} if you'd like to provide your own executor service.
     * </p>
     *
     * @return wrapped instance
     */
    CachedModeledHandle<T> cached();

    /**
     * Same as {@link #cached()} but allows for providing an executor service
     *
     * @param executor thread pool to use for the cache and for read operations
     * @return wrapped instance
     */
    CachedModeledHandle<T> cached(ExecutorService executor);

    /**
     * Return this instance's model spec
     *
     * @return model spec
     */
    ModelSpec<T> modelSpec();

    /**
     * <p>
     *     Return a new Modeled Curator instance with all the same options but applying to the given child node of this Modeled Curator's
     *     path. E.g. if this Modeled Curator instance applies to "/a/b", calling <code>modeled.at("c")</code> returns an instance that applies to
     *     "/a/b/c".
     * </p>
     *
     * <p>
     *     The replacement is the <code>toString()</code> value of child or,
     *     if it implements {@link org.apache.curator.universal.api.NodeName},
     *     the value of <code>nodeName()</code>.
     * </p>
     *
     * @param child child node.
     * @return new Modeled Curator instance
     */
    ModeledHandle<T> child(Object child);

    /**
     * <p>
     *     Return a new Modeled Curator instance with all the same options but applying to the parent node of this Modeled Curator's
     *     path. E.g. if this Modeled Curator instance applies to "/a/b/c", calling <code>modeled.parent()</code> returns an instance that applies to
     *     "/a/b".
     * </p>
     *
     * <p>
     *     The replacement is the <code>toString()</code> value of child or,
     *     if it implements {@link org.apache.curator.universal.api.NodeName},
     *     the value of <code>nodeName()</code>.
     * </p>
     *
     * @return new Modeled Curator instance
     */
    ModeledHandle<T> parent();

    /**
     * Return a Modeled Curator instance with all the same options but using the given path.
     *
     * @param path new path
     * @return new Modeled Curator instance
     */
    ModeledHandle<T> withPath(NodePath path);

    /**
     * Create (or update depending on build options) a ZNode at this instance's path with a serialized
     * version of the given model
     *
     * @param model model to write
     * @return CompletionStage
     */
    CompletionStage<String> set(T model);

    /**
     * Create (or update depending on build options) a ZNode at this instance's path with a serialized
     * version of the given model
     *
     * @param model model to write
     * @param version if data is being set instead of creating the node, the data version to use
     * @return CompletionStage
     */
    CompletionStage<String> set(T model, int version);

    /**
     * Read the ZNode at this instance's path and deserialize into a model
     *
     * @return CompletionStage
     */
    CompletionStage<T> read();

    /**
     * Read the Node at this instance's path and deserialize into a model
     *
     * @return CompletionStage
     */
    CompletionStage<Node<T>> readAsNode();

    /**
     * Delete the ZNode at this instance's path passing -1 for the delete version
     *
     * @return CompletionStage
     */
    CompletionStage<Void> delete();

    /**
     * Delete the ZNode at this instance's path passing the given delete version
     *
     * @param version update version to use
     * @return CompletionStage
     */
    CompletionStage<Void> delete(int version);

    /**
     * Return the child paths of this instance's path (in no particular order)
     *
     * @return CompletionStage
     */
    CompletionStage<List<NodePath>> children();

    /**
     * Return the child paths of this instance's parent path (in no particular order)
     *
     * @return CompletionStage
     */
    CompletionStage<List<NodePath>> siblings();
}
