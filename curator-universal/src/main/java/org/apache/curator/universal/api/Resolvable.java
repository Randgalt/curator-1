package org.apache.curator.universal.api;

import java.util.Arrays;
import java.util.List;

public interface Resolvable
{
    /**
     * When creating paths, any node in the path can be set to {@link NodePath#parameter()}.
     * At runtime, the ZPath can be "resolved" by replacing these nodes with values.
     *
     * @param parameters list of replacements. Must have be the same length as the number of
     *                   parameter nodes in the path
     * @return new resolved ZPath
     */
    default Object resolved(Object... parameters)
    {
        return resolved(Arrays.asList(parameters));
    }

    /**
     * When creating paths, any node in the path can be set to {@link NodePath#parameter()}.
     * At runtime, the ZPath can be "resolved" by replacing these nodes with values.
     *
     * @param parameters list of replacements. Must have be the same length as the number of
     *                   parameter nodes in the path
     * @return new resolved ZPath
     */
    Object resolved(List<Object> parameters);
}
