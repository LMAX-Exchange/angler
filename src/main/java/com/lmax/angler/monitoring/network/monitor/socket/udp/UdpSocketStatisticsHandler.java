package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetAddress;

/**
 * Handler for socket statistics from /proc/net/udp.
 */
@FunctionalInterface
public interface UdpSocketStatisticsHandler
{
    /**
     * Callback method.
     * @param inetAddress the ip address
     * @param port the socket port
     * @param socketIdentifier the socket identifier
     * @param inode the socket inode
     * @param receiveQueueDepth the sampled receive queue depth
     * @param transmitQueueDepth the sampled transmit queue depth
     * @param drops the drop count
     */
    void onStatisticsUpdated(
            final InetAddress inetAddress,
            final int port,
            final long socketIdentifier,
            final long inode,
            final long receiveQueueDepth,
            final long transmitQueueDepth,
            final long drops);
}
