package com.lmax.angler.monitoring.network.monitor.socket.udp;

import org.agrona.collections.LongHashSet;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * Filter class: will only notify delegate if reported socket is owned by the current process.
 */
public final class SocketOwnedByCurrentProcessFilter implements UdpSocketStatisticsHandler
{
    static final int MAX_NOT_OWNED_INODE_CACHE_SIZE = 4096;
    private static final int INITIAL_SOCKET_INODE_CACHE_SIZE = 256;

    private final UdpSocketStatisticsHandler delegate;
    private final Consumer<LongHashSet> socketInodeRetriever;

    private final LongHashSet socketInodesOwnedByThisProcess =
            new LongHashSet(INITIAL_SOCKET_INODE_CACHE_SIZE, Long.MIN_VALUE);
    private final LongHashSet socketInodesNotOwnedByThisProcess =
            new LongHashSet(INITIAL_SOCKET_INODE_CACHE_SIZE, Long.MIN_VALUE);

    public SocketOwnedByCurrentProcessFilter(final UdpSocketStatisticsHandler delegate,
                                             final Consumer<LongHashSet> socketInodeRetriever)
    {
        this.delegate = delegate;
        this.socketInodeRetriever = socketInodeRetriever;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStatisticsUpdated(final InetSocketAddress socketAddress, final int port, final long socketIdentifier,
                                    final long inode, final long receiveQueueDepth, final long drops)
    {
        if(socketInodesNotOwnedByThisProcess.contains(inode))
        {
            return;
        }

        if(!socketInodesOwnedByThisProcess.contains(inode))
        {
            socketInodeRetriever.accept(socketInodesOwnedByThisProcess);
            if(!socketInodesOwnedByThisProcess.contains(inode))
            {
                clearNotOwnedInodeCacheIfTooLarge();
                socketInodesNotOwnedByThisProcess.add(inode);
            }
        }

        if(socketInodesOwnedByThisProcess.contains(inode))
        {
            delegate.onStatisticsUpdated(socketAddress, port, socketIdentifier, inode, receiveQueueDepth, drops);
        }
    }

    private void clearNotOwnedInodeCacheIfTooLarge()
    {
        if(socketInodesNotOwnedByThisProcess.size() >= MAX_NOT_OWNED_INODE_CACHE_SIZE)
        {
            socketInodesNotOwnedByThisProcess.clear();
        }
    }
}
