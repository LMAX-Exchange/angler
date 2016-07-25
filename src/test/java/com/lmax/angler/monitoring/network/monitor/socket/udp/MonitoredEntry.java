package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

final class MonitoredEntry
{
    private final InetSocketAddress socketAddress;
    private final long socketIdentifier;
    private final long inode;
    private final long receiverQueueDepth;
    private final long transmitQueueDepth;
    private final long drops;

    MonitoredEntry(
            final InetAddress inetAddress,
            final int port,
            final long socketIdentifier,
            final long inode,
            final long receiverQueueDepth,
            final long transmitQueueDepth,
            final long drops)
    {
        this.socketAddress = new InetSocketAddress(inetAddress, port);
        this.socketIdentifier = socketIdentifier;
        this.inode = inode;
        this.receiverQueueDepth = receiverQueueDepth;
        this.transmitQueueDepth = transmitQueueDepth;
        this.drops = drops;
    }

    InetSocketAddress getSocketAddress()
    {
        return socketAddress;
    }

    long getSocketIdentifier()
    {
        return socketIdentifier;
    }

    long getReceiverQueueDepth()
    {
        return receiverQueueDepth;
    }

    long getTransmitQueueDepth()
    {
        return transmitQueueDepth;
    }

    long getDrops()
    {
        return drops;
    }

    long getInode()
    {
        return inode;
    }
}
