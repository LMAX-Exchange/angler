package com.epickrram.monitoring.network.monitor.socket.udp;

import java.net.InetSocketAddress;

public interface UdpSocketMonitoringLifecycleListener
{
    void socketMonitoringStarted(final InetSocketAddress socketAddress, final long inode);
    void socketMonitoringStopped(final InetSocketAddress socketAddress, final long inode);
}
