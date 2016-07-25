package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import java.net.InetAddress;

final class TcpBufferStats
{
    private final InetAddress inetAddress;
    private final int port;
    private final long inode;
    private long receiveQueueDepth = -1;
    private long transmitQueueDepth = -1;
    private boolean changed;
    private long updateCount = -1;

    TcpBufferStats(final InetAddress inetAddress, final int port, final long inode)
    {
        this.inetAddress = inetAddress;
        this.port = port;
        this.inode = inode;
    }

    void updateFrom(final TcpStatsEntry entry)
    {
        changed = (this.receiveQueueDepth != entry.getReceiveQueueDepth()) ||
                (this.transmitQueueDepth != entry.getTransmitQueueDepth());
        this.receiveQueueDepth = entry.getReceiveQueueDepth();
        this.transmitQueueDepth = entry.getTransmitQueueDepth();
    }

    boolean hasChanged()
    {
        return changed;
    }

    void updateCount(final long updateCount)
    {
        this.updateCount = updateCount;
    }

    long getUpdateCount()
    {
        return updateCount;
    }

    long getInode()
    {
        return inode;
    }

    int getPort()
    {
        return port;
    }

    InetAddress getInetAddress()
    {
        return inetAddress;
    }
}
