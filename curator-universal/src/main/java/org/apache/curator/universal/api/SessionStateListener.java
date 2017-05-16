package org.apache.curator.universal.api;

@FunctionalInterface
public interface SessionStateListener
{
    void newSessionState(SessionState newState);
}
