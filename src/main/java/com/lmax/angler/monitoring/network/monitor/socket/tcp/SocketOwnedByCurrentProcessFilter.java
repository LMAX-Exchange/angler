package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import org.agrona.collections.LongHashSet;

import java.net.InetAddress;
import java.util.function.Consumer;

/**
 * Filter class: will only notify delegate if reported socket is owned by the current process.
 */
public final class SocketOwnedByCurrentProcessFilter implements TcpSocketStatisticsHandler
{
    static final int MAX_NOT_OWNED_INODE_CACHE_SIZE = 4096;
    private static final int INITIAL_SOCKET_INODE_CACHE_SIZE = 256;

    private final TcpSocketStatisticsHandler delegate;
    private final Consumer<LongHashSet> socketInodeRetriever;

    private final LongHashSet socketInodesOwnedByThisProcess =
            new LongHashSet(INITIAL_SOCKET_INODE_CACHE_SIZE, Long.MIN_VALUE);
    private final LongHashSet socketInodesNotOwnedByThisProcess =
            new LongHashSet(INITIAL_SOCKET_INODE_CACHE_SIZE, Long.MIN_VALUE);

    public SocketOwnedByCurrentProcessFilter(final TcpSocketStatisticsHandler delegate,
                                             final Consumer<LongHashSet> socketInodeRetriever)
    {
        this.delegate = delegate;
        this.socketInodeRetriever = socketInodeRetriever;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStatisticsUpdated(final InetAddress inetAddress, final int port, final long socketIdentifier,
                                    final long inode, final long receiveQueueDepth, final long transmitQueueDepth)
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
            delegate.onStatisticsUpdated(inetAddress, port, socketIdentifier, inode,
                    receiveQueueDepth, transmitQueueDepth);
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
