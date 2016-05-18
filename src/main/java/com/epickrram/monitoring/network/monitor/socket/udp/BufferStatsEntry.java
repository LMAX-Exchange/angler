package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.socket.SocketIdentifier;

final class BufferStatsEntry
{
    private long socketIdentifier;
    private long receiveQueueDepth;
    private long drops;
    private long inode;

    public long getSocketIdentifier()
    {
        return socketIdentifier;
    }

    public long getSocketInstanceIndentifier()
    {
        return SocketIdentifier.overlayInode(socketIdentifier, inode);
    }

    public void setSocketIdentifier(final long socketIdentifier)
    {
        this.socketIdentifier = socketIdentifier;
    }

    public void setInode(final long inode)
    {
        this.inode = inode;
    }

    public long getReceiveQueueDepth()
    {
        return receiveQueueDepth;
    }

    public void setReceiveQueueDepth(final long receiveQueueDepth)
    {
        this.receiveQueueDepth = receiveQueueDepth;
    }

    public long getDrops()
    {
        return drops;
    }

    public void setDrops(final long drops)
    {
        this.drops = drops;
    }

    public long getInode()
    {
        return inode;
    }

    public void reset()
    {
        socketIdentifier = 0L;
        receiveQueueDepth = 0L;
        drops = 0L;
        inode = 0L;
    }
}
