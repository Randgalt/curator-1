package org.apache.curator.universal.consul.details;

class ApiPaths
{
    private static final String base = "/v1";

    static final String createSession = base + "/session/create";
    static final String deleteSession = base + "/session/delete";
    static final String renewSession = base + "/session/renew";

    static final String keyValue = base + "/kv";
}
