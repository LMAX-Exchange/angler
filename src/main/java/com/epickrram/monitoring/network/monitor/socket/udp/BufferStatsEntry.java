package com.epickrram.monitoring.network.monitor.socket.udp;

final class BufferStatsEntry
{
    private long socketIdentifier;
    private long receiveQueueDepth;
    private long drops;

    public long getSocketIdentifier()
    {
        return socketIdentifier;
    }

    public void setSocketIdentifier(final long socketIdentifier)
    {
        this.socketIdentifier = socketIdentifier;
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

    public void reset()
    {
        socketIdentifier = 0L;
        receiveQueueDepth = 0L;
        drops = 0L;
    }
}
