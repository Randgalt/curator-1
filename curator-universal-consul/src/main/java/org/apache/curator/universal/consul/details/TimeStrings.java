package org.apache.curator.universal.consul.details;

import java.time.Duration;

class TimeStrings
{
    static Duration parse(String str)
    {
        if ( str.endsWith("s") )
        {
            return Duration.ofSeconds(toInt(str));
        }

        if ( str.endsWith("m") )
        {
            return Duration.ofMinutes(toInt(str));
        }

        throw new RuntimeException("Could not parse: " + str);
    }

    static String toMinutes(Duration d)
    {
        return d.toMinutes() + "m";
    }

    static String toSeconds(Duration d)
    {
        return (d.toMillis() / 1000) + "s";
    }

    private static int toInt(String str)
    {
        try
        {
            return Integer.parseInt(str.substring(0, str.length() - 1));
        }
        catch ( Exception e )
        {
            throw new RuntimeException("Could not parse: " + str, e);
        }
    }

    private TimeStrings()
    {
    }
}
