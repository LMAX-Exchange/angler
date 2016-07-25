package com.lmax.angler.monitoring.network.monitor.socket.udp;

import com.lmax.angler.monitoring.network.monitor.socket.SocketIdentifier;
import com.lmax.angler.monitoring.network.monitor.util.FileLoader;
import com.lmax.angler.monitoring.network.monitor.util.Parsers;
import com.lmax.angler.monitoring.network.monitor.util.TokenHandler;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongHashSet;
import org.agrona.collections.LongIterator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitor for reporting changes in /proc/net/udp.
 */
public final class UdpSocketMonitor
{
    private static final float AGRONA_DEFAULT_LOAD_FACTOR = 0.67f;

    private final Long2ObjectHashMap<UdpBufferStats> monitoredSocketInstances = new Long2ObjectHashMap<>();
    private final AtomicReference<Long2ObjectHashMap<InetSocketAddress>> candidateSockets = new AtomicReference<>(new Long2ObjectHashMap<>());
    private final LongHashSet keysForRemoval = new LongHashSet(Long.MIN_VALUE);

    private final UdpSocketMonitoringLifecycleListener lifecycleListener;
    private final UdpColumnHandler tokenHandler = new UdpColumnHandler(this::handleEntry);
    private final TokenHandler lineParser = Parsers.rowColumnParser(tokenHandler);

    private final FileLoader fileLoader;

    private UdpSocketStatisticsHandler statisticsHandler;
    private long updateCount = 0;

    public UdpSocketMonitor(final UdpSocketMonitoringLifecycleListener lifecycleListener)
    {
        this(lifecycleListener, Paths.get("/proc/net/udp"));
    }

    UdpSocketMonitor(final UdpSocketMonitoringLifecycleListener lifecycleListener, final Path pathToProcNetUdp)
    {
        this.lifecycleListener = lifecycleListener;
        fileLoader = new FileLoader(pathToProcNetUdp, 65536);
    }

    /**
     * Read from monitored file, report any changed values for monitored socket statistics.
     *
     * Not thread-safe, only call from a single thread.
     *
     * @param handler the callback for socket statistics
     */
    public void poll(final UdpSocketStatisticsHandler handler)
    {
        this.statisticsHandler = handler;
        try
        {
            fileLoader.load();
            final ByteBuffer buffer = fileLoader.getBuffer();

            lineParser.reset();
            lineParser.handleToken(buffer, buffer.position(), buffer.limit());
        }
        finally
        {
            this.statisticsHandler = null;
        }

        purgeStaleEntries();

        updateCount++;
    }

    /**
     * Register interest in a socket.
     *
     * Thread-safe, can be called from multiple threads concurrently.
     *
     * @param socketAddress the socket address
     */
    public void beginMonitoringOf(final InetSocketAddress socketAddress)
    {
        beginMonitoringSocketIdentifier(socketAddress, SocketIdentifier.fromInet4SocketAddress(socketAddress));
    }

    /**
     * De-register interest in a socket.
     *
     * Thread-safe, can be called from multiple threads concurrently.
     *
     * @param socketAddress the socket address
     */
    public void endMonitoringOf(final InetSocketAddress socketAddress)
    {
        endMonitoringOfSocketIdentifier(SocketIdentifier.fromInet4SocketAddress(socketAddress));
    }

    /**
     * Register interest in sockets listening to the specified address on any port.
     *
     * Thread-safe, can be called from multiple threads concurrently.
     *
     * @param inetAddress the IP address
     */
    public void beginMonitoringOf(final InetAddress inetAddress)
    {
        final long socketIdentifier = SocketIdentifier.fromInet4Address(inetAddress);
        beginMonitoringSocketIdentifier(new InetSocketAddress(inetAddress, 0), socketIdentifier);
    }

    /**
     * De-register interest in an IP address.
     *
     * Thread-safe, can be called from multiple threads concurrently.
     *
     * @param inetAddress the IP address
     */
    public void endMonitoringOf(final InetAddress inetAddress)
    {
        endMonitoringOfSocketIdentifier(SocketIdentifier.fromInet4Address(inetAddress));
    }

    private void handleEntry(final UdpStatsEntry entry)
    {
        final long socketIdentifier = entry.getSocketIdentifier();
        final long matchAllPortsSocketIdentifier = SocketIdentifier.asMatchAllSocketsSocketIdentifier(socketIdentifier);
        final Long2ObjectHashMap<InetSocketAddress> candidateSocketsSnapshot = candidateSockets.get();

        if(candidateSocketsSnapshot.containsKey(socketIdentifier) ||
           candidateSocketsSnapshot.containsKey(matchAllPortsSocketIdentifier))
        {
            final long socketInstanceIdentifier = entry.getSocketInstanceIndentifier();
            final int port = SocketIdentifier.extractPortNumber(socketIdentifier);
            if(!monitoredSocketInstances.containsKey(socketInstanceIdentifier))
            {
                InetSocketAddress socketAddress = candidateSocketsSnapshot.get(socketIdentifier);
                if(socketAddress == null)
                {
                    // this is a match-all request
                    // need to construct socket address based on entry
                    socketAddress = candidateSocketsSnapshot.get(matchAllPortsSocketIdentifier);
                }
                monitoredSocketInstances.put(socketInstanceIdentifier,
                        new UdpBufferStats(socketAddress.getAddress(),
                                port, entry.getInode()));
                lifecycleListener.socketMonitoringStarted(socketAddress.getAddress(), port, entry.getInode());
            }
            final UdpBufferStats lastUpdate = monitoredSocketInstances.get(socketInstanceIdentifier);
            lastUpdate.updateFrom(entry);
            lastUpdate.updateCount(updateCount);
            if(lastUpdate.hasChanged())
            {
                statisticsHandler.onStatisticsUpdated(
                        lastUpdate.getInetAddress(),
                        port,
                        entry.getSocketIdentifier(),
                        entry.getInode(),
                        entry.getReceiveQueueDepth(),
                        entry.getTransmitQueueDepth(),
                        entry.getDrops());
            }
        }
    }

    private void purgeStaleEntries()
    {
        keysForRemoval.clear();
        final Long2ObjectHashMap<UdpBufferStats>.KeyIterator iterator = monitoredSocketInstances.keySet().iterator();
        while(iterator.hasNext())
        {
            final long key = iterator.nextLong();
            if(monitoredSocketInstances.get(key).getUpdateCount() != updateCount)
            {
                keysForRemoval.add(key);
            }
        }

        final LongIterator keyIterator = keysForRemoval.iterator();
        while(keyIterator.hasNext())
        {
            final long key = keyIterator.nextValue();
            final UdpBufferStats staleEntry = monitoredSocketInstances.remove(key);
            lifecycleListener.socketMonitoringStopped(staleEntry.getInetAddress(), staleEntry.getPort(), staleEntry.getInode());
        }
    }

    private void beginMonitoringSocketIdentifier(final InetSocketAddress socketAddress, final long socketIdentifier)
    {
        while (true)
        {
            final Long2ObjectHashMap<InetSocketAddress> candidateSnapshot = candidateSockets.get();
            final Long2ObjectHashMap<InetSocketAddress> updated = new Long2ObjectHashMap<>(candidateSnapshot.size(), AGRONA_DEFAULT_LOAD_FACTOR);

            final Long2ObjectHashMap<InetSocketAddress>.KeyIterator keyIterator = candidateSnapshot.keySet().iterator();
            while(keyIterator.hasNext())
            {
                final long key = keyIterator.nextLong();
                updated.put(key, candidateSnapshot.get(key));
            }

            updated.put(socketIdentifier, socketAddress);

            if (candidateSockets.compareAndSet(candidateSnapshot, updated))
            {
                break;
            }
        }
    }

    private void endMonitoringOfSocketIdentifier(final long socketIdentifier)
    {
        while (true)
        {
            final Long2ObjectHashMap<InetSocketAddress> candidateSnapshot = candidateSockets.get();
            final Long2ObjectHashMap<InetSocketAddress> updated = new Long2ObjectHashMap<>(candidateSnapshot.size(), AGRONA_DEFAULT_LOAD_FACTOR);

            final Long2ObjectHashMap<InetSocketAddress>.KeyIterator keyIterator = candidateSnapshot.keySet().iterator();
            while(keyIterator.hasNext())
            {
                final long key = keyIterator.nextLong();
                if(key != socketIdentifier)
                {
                    updated.put(key, candidateSnapshot.get(key));
                }
            }

            if (candidateSockets.compareAndSet(candidateSnapshot, updated))
            {
                break;
            }
        }
    }
}