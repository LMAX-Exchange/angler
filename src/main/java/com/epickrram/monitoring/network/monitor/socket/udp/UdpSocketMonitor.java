package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.socket.SocketIdentifier;
import com.epickrram.monitoring.network.monitor.util.DelimitedDataParser;
import com.epickrram.monitoring.network.monitor.util.FileLoader;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongHashSet;
import org.agrona.collections.LongIterator;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public final class UdpSocketMonitor
{
    private static final double AGRONA_DEFAULT_LOAD_FACTOR = 0.67;

    private final Long2ObjectHashMap<UdpBufferStats> monitoredSocketInstances = new Long2ObjectHashMap<>();
    private final AtomicReference<Long2ObjectHashMap<InetSocketAddress>> candidateSockets = new AtomicReference<>(new Long2ObjectHashMap<>());
    private final LongHashSet keysForRemoval = new LongHashSet(Long.MIN_VALUE);

    private final UdpSocketMonitoringLifecycleListener lifecycleListener;
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new UdpColumnHandler(this::handleEntry), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);
    private final FileLoader fileLoader;

    private UdpSocketStatisticsHandler statisticsHandler;
    private long updateCount = 0;

    public UdpSocketMonitor(final UdpSocketMonitoringLifecycleListener lifecycleListener, final Path pathToProcNetUdp)
    {
        this.lifecycleListener = lifecycleListener;
        fileLoader = new FileLoader(65536, pathToProcNetUdp);
    }

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

    public void beginMonitoringOf(final InetSocketAddress socketAddress)
    {
        final long socketIdentifier = SocketIdentifier.fromInet4SocketAddress(socketAddress);

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

    public void endMonitoringOf(final InetSocketAddress socketAddress)
    {
        final long socketIdentifier = SocketIdentifier.fromInet4SocketAddress(socketAddress);

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

    private void handleEntry(final BufferStatsEntry entry)
    {
        final long socketIdentifier = entry.getSocketIdentifier();
        final Long2ObjectHashMap<InetSocketAddress> candidateSocketsSnapshot = candidateSockets.get();
        if(candidateSocketsSnapshot.containsKey(socketIdentifier))
        {
            final long socketInstanceIdentifier = entry.getSocketInstanceIndentifier();
            if(!monitoredSocketInstances.containsKey(socketInstanceIdentifier))
            {
                monitoredSocketInstances.put(socketInstanceIdentifier,
                        new UdpBufferStats(candidateSocketsSnapshot.get(socketIdentifier), entry.getInode()));
                lifecycleListener.socketMonitoringStarted(candidateSocketsSnapshot.get(socketIdentifier), entry.getInode());
            }
            final UdpBufferStats lastUpdate = monitoredSocketInstances.get(socketInstanceIdentifier);
            lastUpdate.updateFrom(entry);
            lastUpdate.updateCount(updateCount);
            if(lastUpdate.hasChanged())
            {
                statisticsHandler.onStatisticsUpdated(lastUpdate.socketAddress, entry.getSocketIdentifier(),
                        entry.getInode(), entry.getReceiveQueueDepth(), entry.getDrops());
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
            lifecycleListener.socketMonitoringStopped(staleEntry.getSocketAddress(), staleEntry.getInode());
        }
    }

    private static final class UdpBufferStats
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
}