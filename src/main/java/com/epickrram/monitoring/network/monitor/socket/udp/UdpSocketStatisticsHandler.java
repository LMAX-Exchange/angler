package com.epickrram.monitoring.network.monitor.socket.udp;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface UdpSocketStatisticsHandler
{
    void onStatisticsUpdated(
            final InetSocketAddress socketAddress,
            final long socketIdentifier,
            final long inode,
            final long receiveQueueDepth,
            final long drops);
}
