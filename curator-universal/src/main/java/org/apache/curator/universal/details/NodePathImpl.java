package org.apache.curator.universal.details;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.curator.universal.api.NodeName;
import org.apache.curator.universal.api.NodePath;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NodePathImpl implements NodePath
{
    public static final NodePath root = new NodePathImpl(Collections.singletonList(pathSeparator), null);

    private final List<String> nodes;
    private final boolean isResolved;
    private volatile String fullPath = null;
    private volatile NodePath parent = null;
    private volatile Pattern schema = null;

    public static NodePath parse(String fullPath, UnaryOperator<String> nameFilter)
    {
        return parseInternal(fullPath, nameFilter);
    }

    private static NodePathImpl parseInternal(String fullPath, UnaryOperator<String> nameFilter)
    {
        List<String> nodes = ImmutableList.<String>builder()
            .add(pathSeparator)
            .addAll(
                Splitter.on(pathSeparator)
                    .omitEmptyStrings()
                    .splitToList(fullPath)
                    .stream()
                    .map(nameFilter)
                    .collect(Collectors.toList())
             )
            .build();
        nodes.forEach(NodePathImpl::validate);
        return new NodePathImpl(nodes, null);
    }

    public static NodePath from(String[] names)
    {
        return from(null, Arrays.asList(names));
    }

    public static NodePath from(List<String> names)
    {
        return from(null, names);
    }

    public static NodePath from(NodePath base, String[] names)
    {
        return from(base, Arrays.asList(names));
    }

    public static NodePath from(NodePath base, List<String> names)
    {
        names = Objects.requireNonNull(names, "names cannot be null");
        names.forEach(NodePathImpl::validate);
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if ( base != null )
        {
            if ( base instanceof NodePathImpl )
            {
                builder.addAll(((NodePathImpl)base).nodes);
            }
            else
            {
                builder.addAll(Splitter.on(pathSeparator).omitEmptyStrings().splitToList(base.fullPath()));
            }
        }
        else
        {
            builder.add(pathSeparator);
        }
        List<String> nodes = builder.addAll(names).build();
        return new NodePathImpl(nodes, null);
    }

    @Override
    public NodePath child(Object child)
    {
        return new NodePathImpl(nodes, NodeName.nameFrom(child));
    }

    @Override
    public NodePath parent()
    {
        checkRootAccess();
        if ( parent == null )
        {
            parent = new NodePathImpl(nodes.subList(0, nodes.size() - 1), null);
        }
        return parent;
    }

    @Override
    public boolean isRoot()
    {
        return nodes.size() == 1;
    }

    @Override
    public boolean startsWith(NodePath path)
    {
        NodePathImpl rhs;
        if ( path instanceof NodePathImpl )
        {
            rhs = (NodePathImpl)path;
        }
        else
        {
            rhs = parseInternal(path.fullPath(), s -> s);
        }
        return (nodes.size() >= rhs.nodes.size()) && nodes.subList(0, rhs.nodes.size()).equals(rhs.nodes);
    }

    @Override
    public Pattern toPattern()
    {
        if ( schema == null )
        {
            schema = Pattern.compile(buildFullPath(s -> isParameter(s) ? ".*" : s));
        }
        return schema;
    }

    @Override
    public String fullPath()
    {
        checkResolved();
        if ( fullPath == null )
        {
            fullPath = buildFullPath(s -> s);
        }
        return fullPath;
    }

    @Override
    public String nodeName()
    {
        return nodes.get(nodes.size() - 1);
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        NodePathImpl nodePath = (NodePathImpl)o;

        return nodes.equals(nodePath.nodes);
    }

    @Override
    public int hashCode()
    {
        return nodes.hashCode();
    }

    @Override
    public String toString()
    {
        return nodes.subList(1, nodes.size())
            .stream().map(name -> isParameter(name) ? name.substring(1) : name)
            .collect(Collectors.joining(pathSeparator, pathSeparator, ""));
    }

    @Override
    public NodePath resolved(List<Object> parameters)
    {
        Iterator<Object> iterator = parameters.iterator();
        List<String> nodeNames = nodes.stream()
            .map(name -> {
                if ( isParameter(name) && iterator.hasNext() )
                {
                    return NodeName.nameFrom(iterator.next());
                }
                return name;
            })
            .collect(Collectors.toList());
        return new NodePathImpl(nodeNames, null);
    }

    @Override
    public boolean isResolved()
    {
        return isResolved;
    }

    private static boolean isParameter(String name)
    {
        return (name.length() > 1) && name.startsWith(pathSeparator);
    }

    private NodePathImpl(List<String> nodes, String child)
    {
        ImmutableList.Builder<String> builder = ImmutableList.<String>builder().addAll(nodes);
        if ( child != null )
        {
            validate(child);
            builder.add(child);
        }
        this.nodes = builder.build();
        isResolved = this.nodes.stream().noneMatch(NodePathImpl::isParameter);
    }

    private void checkRootAccess()
    {
        if ( isRoot() )
        {
            throw new NoSuchElementException("The root has no parent");
        }
    }

    private void checkResolved()
    {
        if ( !isResolved)
        {
            throw new IllegalStateException("This NodePath has not been resolved: " + toString());
        }
    }

    private static void validate(String nodeName)
    {
        if ( isParameter(Objects.requireNonNull(nodeName, "nodeName cannot be null")) )
        {
            return;
        }
        if ( nodeName.equals(pathSeparator) )
        {
            return;
        }
        // PathUtils.validatePath(pathSeparator + nodeName); TODO
    }

    private String buildFullPath(UnaryOperator<String> filter)
    {
        boolean addSeparator = false;
        StringBuilder str = new StringBuilder();
        int size = nodes.size();
        int parameterIndex = 0;
        for ( int i = 0; i < size; ++i )
        {
            if ( i > 1 )
            {
                str.append(pathSeparator);
            }
            str.append(filter.apply(nodes.get(i)));
        }
        return str.toString();
    }
}
