package com.lmax.angler.monitoring.network.monitor.socket.udp;

import org.agrona.collections.LongHashSet;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SocketOwnedByCurrentProcessFilterTest
{
    private static final long INODE_OWNED_BY_PROCESS = 8765542L;
    private static final long INODE_NOT_OWNED_BY_PROCESS = Long.MAX_VALUE;
    private static final int PORT = 55555;
    private static final InetSocketAddress SOCKET_ADDRESS = new InetSocketAddress(PORT);

    private final UdpSocketStatisticsHandler delegate = this::handleStatistics;
    private final SocketOwnedByCurrentProcessFilter filter =
            new SocketOwnedByCurrentProcessFilter(delegate, this::populateSocketInodes);

    private int receivedUpdateCount;
    private int socketInodeRequestCount;

    @Test
    public void shouldNotNotifyDelegateIfInodeIsNotSocketOwnedByCurrentProcess() throws Exception
    {
        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, INODE_NOT_OWNED_BY_PROCESS, 0L, 0L);

        assertThat(receivedUpdateCount, is(0));
    }

    @Test
    public void shouldNotifyDelegateIfInodeIsSocketOwnedByCurrentProcess() throws Exception
    {
        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, INODE_OWNED_BY_PROCESS, 0L, 0L);

        assertThat(receivedUpdateCount, is(1));
    }

    @Test
    public void shouldNotRequestInodeUpdateIfInodeIsAlreadyKnown() throws Exception
    {
        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, INODE_OWNED_BY_PROCESS, 0L, 0L);
        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, INODE_OWNED_BY_PROCESS, 0L, 0L);

        assertThat(receivedUpdateCount, is(2));
        assertThat(socketInodeRequestCount, is(1));
    }

    @Test
    public void shouldNotRequestInodeUpdateOfNotOwnedInodeWhenInodeHasBeenRequestedBefore() throws Exception
    {
        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, INODE_NOT_OWNED_BY_PROCESS, 0L, 0L);
        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, INODE_NOT_OWNED_BY_PROCESS, 0L, 0L);

        assertThat(receivedUpdateCount, is(0));
        assertThat(socketInodeRequestCount, is(1));
    }

    @Test
    public void shouldClearNotOwnedInodeCacheToPreventMemoryLeak() throws Exception
    {
        final int initialNotOwnedInode = 0;

        for(int i = 0; i < SocketOwnedByCurrentProcessFilter.MAX_NOT_OWNED_INODE_CACHE_SIZE + 1; i++)
        {
            filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, i, 0L, 0L);
        }

        socketInodeRequestCount = 0;

        filter.onStatisticsUpdated(SOCKET_ADDRESS, PORT, 17L, initialNotOwnedInode, 0L, 0L);

        assertThat(socketInodeRequestCount, is(1));
    }

    private void handleStatistics(final InetSocketAddress socketAddress,
                                  final int port,
                                  final long socketIdentifier,
                                  final long inode,
                                  final long receiveQueueDepth,
                                  final long drops)
    {
        receivedUpdateCount++;
    }

    private void populateSocketInodes(final LongHashSet socketInodes)
    {
        socketInodes.add(INODE_OWNED_BY_PROCESS);
        socketInodeRequestCount++;
    }
}