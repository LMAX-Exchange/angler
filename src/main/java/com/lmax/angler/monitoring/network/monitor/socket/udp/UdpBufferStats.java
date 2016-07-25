package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetAddress;

final class UdpBufferStats
{
    private final InetAddress inetAddress;
    private final int port;
    private final long inode;
    private long receiveQueueDepth = -1;
    private long transmitQueueDepth = -1;
    private long drops = -1;
    private boolean changed;
    private long updateCount = -1;

    UdpBufferStats(final InetAddress inetAddress, final int port, final long inode)
    {
        this.inetAddress = inetAddress;
        this.port = port;
        this.inode = inode;
    }

    void updateFrom(final BufferStatsEntry entry)
    {
        changed = (this.receiveQueueDepth != entry.getReceiveQueueDepth()) ||
                (this.drops != entry.getDrops()) ||
                (this.transmitQueueDepth != entry.getTransmitQueueDepth());
        this.receiveQueueDepth = entry.getReceiveQueueDepth();
        this.transmitQueueDepth = entry.getTransmitQueueDepth();
        this.drops = entry.getDrops();
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
