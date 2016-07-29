package com.lmax.angler.monitoring.network.monitor;

import com.lmax.angler.monitoring.network.monitor.socket.SocketDescriptor;

public interface DescribableSocket extends Updated
{
    void describeTo(final SocketDescriptor descriptor);
}
