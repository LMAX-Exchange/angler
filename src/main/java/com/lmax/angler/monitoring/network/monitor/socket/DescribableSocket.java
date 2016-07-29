package com.lmax.angler.monitoring.network.monitor.socket;

public interface DescribableSocket extends Updated
{
    void describeTo(final SocketDescriptor descriptor);
}
