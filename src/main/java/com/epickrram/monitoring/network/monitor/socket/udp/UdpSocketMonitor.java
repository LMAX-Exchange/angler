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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class UdpSocketMonitor
{
    private static final Path PROC_NET_UDP = Paths.get("/proc/net/udp");

    // TODO replace with Long2ObjectMap
    private final AtomicReference<Map<Long, UdpBufferStats>> mapReference = new AtomicReference<>(new HashMap<>());

    private final UdpSocketMonitoringLifecycleListener lifecycleListener;
    private final Path pathToProcNetUdp;
    private final BufferStatsEntry statsEntry = new BufferStatsEntry();
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new ColumnHandler(this::handleEntry), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);

    private ByteBuffer buffer = ByteBuffer.allocateDirect(65536);
    private FileChannel fileChannel;
    private UdpSocketStatisticsHandler statisticsHandler;
    private Map<Long, UdpBufferStats> monitoredSocketsSnapshot;

    public UdpSocketMonitor(final UdpSocketMonitoringLifecycleListener lifecycleListener, final Path pathToProcNetUdp)
    {
        this.lifecycleListener = lifecycleListener;
        this.pathToProcNetUdp = pathToProcNetUdp;
    }

    private void handleEntry(final BufferStatsEntry entry)
    {
        // TODO get inet address from map
        final UdpBufferStats lastUpdate = monitoredSocketsSnapshot.get(entry.getSocketIdentifier());
        if(lastUpdate != null)
        {
            statisticsHandler.onStatisticsUpdated(lastUpdate.socketAddress, entry.getSocketIdentifier(),
                    entry.getInode(), entry.getReceiveQueueDepth(), entry.getDrops());
        }
    }

    public void poll(final UdpSocketStatisticsHandler handler)
    {
        this.statisticsHandler = handler;
        this.monitoredSocketsSnapshot = mapReference.get();
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
            fileChannel.position(0);
            fileChannel.read(buffer);
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
    }


    public void beginMonitoringOf(final InetSocketAddress socketAddress)
    {
        final Map<Long, UdpBufferStats> currentSockets = mapReference.get();
        final long socketIdentifier = SocketIdentifier.fromInet4SocketAddress(socketAddress);

        if(currentSockets.containsKey(socketIdentifier))
        {
            return;
        }
        while(true)
        {
            final Map<Long, UdpBufferStats> updatedSockets = new HashMap<>(currentSockets.size());
            updatedSockets.putAll(currentSockets);
            updatedSockets.put(socketIdentifier, new UdpBufferStats(0L, 0L, socketAddress));
            if(mapReference.compareAndSet(currentSockets, updatedSockets))
            {
                break;
            }
        }
        lifecycleListener.socketMonitoringStarted(socketAddress);
    }

    public void endMonitoringOf(final InetSocketAddress socketAddress)
    {
        final Map<Long, UdpBufferStats> currentSockets = mapReference.get();
        final long socketIdentifier = SocketIdentifier.fromInet4SocketAddress(socketAddress);

        if(!currentSockets.containsKey(socketIdentifier))
        {
            return;
        }
        while(true)
        {
            final Map<Long, UdpBufferStats> updatedSockets = new HashMap<>(currentSockets.size());
            updatedSockets.putAll(currentSockets);
            updatedSockets.remove(socketIdentifier);
            if(mapReference.compareAndSet(currentSockets, updatedSockets))
            {
                break;
            }
        }
        lifecycleListener.socketMonitoringStopped(socketAddress);
    }

    private static final class UdpBufferStats
    {
        private long receiveQueueDepth;
        private long drops;
        private boolean changed;
        private InetSocketAddress socketAddress;

        public UdpBufferStats(final long receiveQueueDepth, final long drops, final InetSocketAddress socketAddress)
        {
            this.receiveQueueDepth = receiveQueueDepth;
            this.drops = drops;
            this.socketAddress = socketAddress;
        }

        void update(final long receiveQueueDepth, final long drops)
        {
            changed = (this.receiveQueueDepth != receiveQueueDepth) || (this.drops != drops);
            this.receiveQueueDepth = receiveQueueDepth;
            this.drops = drops;
        }
    }
}