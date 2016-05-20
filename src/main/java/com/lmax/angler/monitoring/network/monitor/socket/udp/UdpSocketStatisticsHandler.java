package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetSocketAddress;

/**
 * Handler for socket statistics from /proc/net/udp.
 */
@FunctionalInterface
public interface UdpSocketStatisticsHandler
{
    /**
     * Callback method.
     * @param socketAddress the socket address
     * @param socketIdentifier the socket identifier
     * @param inode the socket inode
     * @param receiveQueueDepth the sampled receive queue depth
     * @param drops the drop count
     */
    void onStatisticsUpdated(
            final InetSocketAddress socketAddress,
            final long socketIdentifier,
            final long inode,
            final long receiveQueueDepth,
            final long drops);
}
