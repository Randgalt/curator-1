package org.apache.curator.universal.modeled;

import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.api.Resolvable;
import java.util.List;

/**
 * A full specification for dealing with a portion of the ZooKeeper tree. ModelSpec's contain:
 *
 * <ul>
 *     <li>A node path</li>
 *     <li>Serializer for the data stored</li>
 *     <li>Options for how to create the node (mode, compression, etc.)</li>
 *     <li>Options for how to deleting the node (quietly, guaranteed, etc.)</li>
 *     <li>ACLs</li>
 *     <li>Optional schema generation</li>
 * </ul>
 */
public interface ModelSpec<T> extends Resolvable
{
    /**
     * <p>
     *     Return a new CuratorModel instance with all the same options but applying to the given child node of this CuratorModel's
     *     path. E.g. if this CuratorModel instance applies to "/a/b", calling <code>modeled.at("c")</code> returns an instance that applies to
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
     * @return new Modeled Spec instance
     */
    ModelSpec<T> child(Object child);

    /**
     * <p>
     *     Return a new CuratorModel instance with all the same options but applying to the parent node of this CuratorModel's
     *     path. E.g. if this CuratorModel instance applies to "/a/b/c", calling <code>modeled.parent()</code> returns an instance that applies to
     *     "/a/b".
     * </p>
     *
     * <p>
     *     The replacement is the <code>toString()</code> value of child or,
     *     if it implements {@link org.apache.curator.universal.api.NodeName},
     *     the value of <code>nodeName()</code>.
     * </p>
     *
     * @return new Modeled Spec instance
     */
    ModelSpec<T> parent();

    /**
     * Return a new CuratorModel instance with all the same options but using the given path.
     *
     * @param path new path
     * @return new Modeled Spec instance
     */
    ModelSpec<T> withPath(NodePath path);

    /**
     * <p>
     *     Return a new CuratorModel instance with all the same options but using a resolved
     *     path by calling {@link org.apache.curator.universal.api.NodePath#resolved(Object...)}
     *     using the given parameters
     * </p>
     *
     * <p>
     *     The replacement is the <code>toString()</code> value of the parameter object or,
     *     if the object implements {@link org.apache.curator.universal.api.NodeName},
     *     the value of <code>nodeName()</code>.
     * </p>
     *
     * @param parameters list of replacements. Must have be the same length as the number of
     *                   parameter nodes in the path
     * @return new resolved ModelSpec
     */
    @Override
    ModelSpec<T> resolved(Object... parameters);

    /**
     * <p>
     *     Return a new CuratorModel instance with all the same options but using a resolved
     *     path by calling {@link org.apache.curator.universal.api.NodePath#resolved(java.util.List)}
     *     using the given parameters
     * </p>
     *
     * <p>
     *     The replacement is the <code>toString()</code> value of the parameter object or,
     *     if the object implements {@link org.apache.curator.universal.api.NodeName},
     *     the value of <code>nodeName()</code>.
     * </p>
     *
     * @param parameters list of replacements. Must have be the same length as the number of
     *                   parameter nodes in the path
     * @return new resolved ModelSpec
     */
    @Override
    ModelSpec<T> resolved(List<Object> parameters);

    /**
     * Return the model's path
     *
     * @return path
     */
    NodePath path();

    /**
     * Return the model's serializer
     *
     * @return serializer
     */
    ModelSerializer<T> serializer();

    /**
     * Return the model's create mode
     *
     * @return create mode
     */
    //CreateMode createMode();

    /**
     * Return the model's ACL list
     *
     * @return ACL list
     */
    //List<ACL> aclList();

    /**
     * Return the model's create options
     *
     * @return create options
     */
    //Set<CreateOption> createOptions();

    /**
     * Return the model's delete options
     *
     * @return delete options
     */
    //Set<DeleteOption> deleteOptions();

    /**
     * Return the TTL to use or -1
     *
     * @return ttl
     */
    //long ttl();
}
