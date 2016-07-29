package com.lmax.angler.monitoring.network.monitor.socket;

/**
 * Represents an entity that can describe itself to a SocketDescriptor.
 */
public interface DescribableSocket extends Updated
{
    void describeTo(final SocketDescriptor descriptor);
}
