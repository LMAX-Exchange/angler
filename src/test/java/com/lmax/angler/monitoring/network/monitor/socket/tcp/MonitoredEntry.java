package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

final class MonitoredEntry
{
    private final InetSocketAddress socketAddress;
    private final long socketIdentifier;
    private final long inode;
    private final long receiverQueueDepth;
    private final long transmitQueueDepth;

    MonitoredEntry(
            final InetAddress inetAddress,
            final int port,
            final long socketIdentifier,
            final long inode,
            final long receiverQueueDepth,
            final long transmitQueueDepth)
    {
        this.socketAddress = new InetSocketAddress(inetAddress, port);
        this.socketIdentifier = socketIdentifier;
        this.inode = inode;
        this.receiverQueueDepth = receiverQueueDepth;
        this.transmitQueueDepth = transmitQueueDepth;
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

    long getInode()
    {
        return inode;
    }
}
