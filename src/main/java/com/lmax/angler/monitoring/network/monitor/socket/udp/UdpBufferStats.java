package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetSocketAddress;

final class UdpBufferStats
{
    private final InetSocketAddress socketAddress;
    private final long inode;
    private long receiveQueueDepth = -1;
    private long drops = -1;
    private boolean changed;
    private long updateCount = -1;

    UdpBufferStats(final InetSocketAddress socketAddress, final long inode)
    {
        this.socketAddress = socketAddress;
        this.inode = inode;
    }

    void updateFrom(final BufferStatsEntry entry)
    {
        changed = (this.receiveQueueDepth != entry.getReceiveQueueDepth()) ||
                (this.drops != entry.getDrops());
        this.receiveQueueDepth = entry.getReceiveQueueDepth();
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

    InetSocketAddress getSocketAddress()
    {
        return socketAddress;
    }
}
