package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.socket.SocketIdentifier;
import com.epickrram.monitoring.network.monitor.util.DelimitedDataParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class UdpSocketMonitor
{
    // TODO replace with Long2ObjectMap
    private final Map<Long, UdpBufferStats> monitoredSocketInstances = new HashMap<>();
    private final AtomicReference<Map<Long, InetSocketAddress>> candidateSockets = new AtomicReference<>(new HashMap<>());

    private final UdpSocketMonitoringLifecycleListener lifecycleListener;
    private final Path pathToProcNetUdp;
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new ColumnHandler(this::handleEntry), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);

    private ByteBuffer buffer = ByteBuffer.allocateDirect(65536);
    private FileChannel fileChannel;
    private UdpSocketStatisticsHandler statisticsHandler;
    private long updateCount = 0;

    public UdpSocketMonitor(final UdpSocketMonitoringLifecycleListener lifecycleListener, final Path pathToProcNetUdp)
    {
        this.lifecycleListener = lifecycleListener;
        this.pathToProcNetUdp = pathToProcNetUdp;
    }

    private void handleEntry(final BufferStatsEntry entry)
    {
        final long socketIdentifier = entry.getSocketIdentifier();
        final Map<Long, InetSocketAddress> candidateSocketsSnapshot = candidateSockets.get();
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

    public void poll(final UdpSocketStatisticsHandler handler)
    {
        this.statisticsHandler = handler;
        try
        {
            if(fileChannel == null)
            {
                fileChannel = FileChannel.open(pathToProcNetUdp, StandardOpenOption.READ);
            }
            final long fileSize = Files.size(pathToProcNetUdp);

            if(fileSize > buffer.capacity())
            {
                buffer = ByteBuffer.allocateDirect(buffer.capacity() * 2);
            }

            buffer.clear();
            fileChannel.read(buffer, 0);
            buffer.flip();

            lineParser.reset();
            lineParser.handleToken(buffer, buffer.position(), buffer.limit());
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
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
            final Map<Long, InetSocketAddress> candidateSnapshot = candidateSockets.get();
            final Map<Long, InetSocketAddress> updated = new HashMap<>();
            updated.putAll(candidateSnapshot);
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
            final Map<Long, InetSocketAddress> candidateSnapshot = candidateSockets.get();
            final Map<Long, InetSocketAddress> updated = new HashMap<>();
            updated.putAll(candidateSnapshot);
            updated.remove(socketIdentifier);

            if (candidateSockets.compareAndSet(candidateSnapshot, updated))
            {
                break;
            }
        }
    }

    private void purgeStaleEntries()
    {
        final Set<Long> keysForRemoval = new HashSet<>();
        for(final Long key : monitoredSocketInstances.keySet())
        {
            if(monitoredSocketInstances.get(key).getUpdateCount() != updateCount)
            {
                keysForRemoval.add(key);
            }
        }

        for(final Long key : keysForRemoval)
        {
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