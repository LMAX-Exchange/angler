package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import com.lmax.angler.monitoring.network.monitor.socket.SocketIdentifier;

/**
 * Value object.
 */
final class TcpStatsEntry
{
    private long socketIdentifier;
    private long receiveQueueDepth;
    private long transmitQueueDepth;
    private long inode;

    long getSocketIdentifier()
    {
        return socketIdentifier;
    }

    long getSocketInstanceIndentifier()
    {
        return SocketIdentifier.overlayInode(socketIdentifier, inode);
    }

    void setSocketIdentifier(final long socketIdentifier)
    {
        this.socketIdentifier = socketIdentifier;
    }

    void setInode(final long inode)
    {
        this.inode = inode;
    }

    long getReceiveQueueDepth()
    {
        return receiveQueueDepth;
    }

    void setReceiveQueueDepth(final long receiveQueueDepth)
    {
        this.receiveQueueDepth = receiveQueueDepth;
    }

    long getTransmitQueueDepth()
    {
        return transmitQueueDepth;
    }

    void setTransmitQueueDepth(final long transmitQueueDepth)
    {
        this.transmitQueueDepth = transmitQueueDepth;
    }

    long getInode()
    {
        return inode;
    }

    void reset()
    {
        socketIdentifier = 0L;
        receiveQueueDepth = 0L;
        transmitQueueDepth = 0L;
        inode = 0L;
    }

}
