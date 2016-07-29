package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.InetAddress;

/**
 * Value object
 */
public final class SocketDescriptor
{
    private InetAddress address;
    private int port;
    private long inode;

    public void set(final InetAddress address, final int port, final long inode)
    {
        this.address = address;
        this.port = port;
        this.inode = inode;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    public long getInode()
    {
        return inode;
    }
}
