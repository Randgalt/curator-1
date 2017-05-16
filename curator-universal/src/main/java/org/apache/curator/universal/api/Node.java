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

/**
 * Abstracts a ZooKeeper node
 */
public interface Node<T>
{
    /**
     * The path of the node
     *
     * @return path
     */
    NodePath path();

    Metadata metadata();

    /**
     * The node's current value
     *
     * @return value
     */
    T value();

    static Node<byte[]> raw(NodePath path, Metadata metadata, byte[] bytes)
    {
        return new Node<byte[]>()
        {
            @Override
            public NodePath path()
            {
                return path;
            }

            @Override
            public Metadata metadata()
            {
                return metadata;
            }

            @Override
            public byte[] value()
            {
                return bytes;
            }
        };
    }
}
