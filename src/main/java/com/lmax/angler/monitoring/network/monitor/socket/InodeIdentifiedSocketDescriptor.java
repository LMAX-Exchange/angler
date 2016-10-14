package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.InetSocketAddress;

public final class InodeIdentifiedSocketDescriptor
{
    private InetSocketAddress socketAddress;
    private long inode;

    public void set(final InetSocketAddress socketAddress, final long inode)
    {
        this.socketAddress = socketAddress;
        this.inode = inode;
    }

    InetSocketAddress getSocketAddress()
    {
        return socketAddress;
    }

    long getInode()
    {
        return inode;
    }
}
