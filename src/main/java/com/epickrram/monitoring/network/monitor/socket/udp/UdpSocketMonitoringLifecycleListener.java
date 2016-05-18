package com.epickrram.monitoring.network.monitor.socket.udp;

import java.net.InetSocketAddress;

public interface UdpSocketMonitoringLifecycleListener
{
    void socketMonitoringStarted(final InetSocketAddress socketAddress);
    void socketMonitoringStopped(final InetSocketAddress socketAddress);
}
