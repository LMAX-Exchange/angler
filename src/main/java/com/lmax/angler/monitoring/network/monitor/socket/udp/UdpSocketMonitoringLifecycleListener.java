package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetSocketAddress;

/**
 * Callback for notifications when a socket becomes available/unavailable for monitoring.
 */
public interface UdpSocketMonitoringLifecycleListener
{
    /**
     * Socket is available for monitoring.
     * @param socketAddress the socket address
     * @param inode the inode of the socket
     */
    void socketMonitoringStarted(final InetSocketAddress socketAddress, final long inode);

    /**
     * Socket is unavailable for monitoring.
     * @param socketAddress the socket address
     * @param inode the inode of the socket
     */
    void socketMonitoringStopped(final InetSocketAddress socketAddress, final long inode);
}
