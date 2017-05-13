package org.apache.curator.universal.modeled;

import org.apache.curator.universal.api.CuratorHandle;
import org.apache.curator.universal.api.NodePath;
import java.util.concurrent.CompletionStage;

public interface ModeledHandle<T>
{
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
     * @return AsyncStage
     */
    CompletionStage<String> set(T model);

    /**
     * Create (or update depending on build options) a ZNode at this instance's path with a serialized
     * version of the given model
     *
     * @param model model to write
     * @param version if data is being set instead of creating the node, the data version to use
     * @return AsyncStage
     */
    CompletionStage<String> set(T model, int version);

    /**
     * Read the ZNode at this instance's path and deserialize into a model
     *
     * @return AsyncStage
     */
    CompletionStage<T> read();

    /**
     * Read the Node at this instance's path and deserialize into a model
     *
     * @return AsyncStage
     */
    CompletionStage<Node<T>> readAsNode();

    /**
     * Delete the ZNode at this instance's path passing -1 for the delete version
     *
     * @return AsyncStage
     */
    CompletionStage<Void> delete();

    /**
     * Delete the ZNode at this instance's path passing the given delete version
     *
     * @param version update version to use
     * @return AsyncStage
     */
    CompletionStage<Void> delete(int version);
}
