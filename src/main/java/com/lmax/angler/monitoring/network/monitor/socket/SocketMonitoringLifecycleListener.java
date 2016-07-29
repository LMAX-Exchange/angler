package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.InetAddress;

/**
 * Callback for notifications when a socket becomes available/unavailable for monitoring.
 */
public interface SocketMonitoringLifecycleListener
{
    /**
     * Socket is available for monitoring.
     * @param inetAddress the ip address
     * @param port the port
     * @param inode the inode of the socket
     */
    void socketMonitoringStarted(final InetAddress inetAddress, final int port, final long inode);

    /**
     * Socket is unavailable for monitoring.
     * @param inetAddress the ip address
     * @param port the port
     * @param inode the inode of the socket
     */
    void socketMonitoringStopped(final InetAddress inetAddress, final int port, final long inode);
}
