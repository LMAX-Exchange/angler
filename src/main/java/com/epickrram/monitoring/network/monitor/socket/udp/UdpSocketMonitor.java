package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.util.DelimitedDataParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
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

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toHexString;

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

    public UdpSocketMonitor(final UdpSocketMonitoringLifecycleListener lifecycleListener, final Path pathToProcNetUdp)
    {
        this.lifecycleListener = lifecycleListener;
        this.pathToProcNetUdp = pathToProcNetUdp;
    }

    private void handleEntry(final BufferStatsEntry entry)
    {

    }

    public void poll(final UdpSocketStatisticsHandler handler)
    {
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

            final Map<Long, UdpBufferStats> currentSockets = mapReference.get();
//            String line;
//            while((line = reader.readLine()) != null)
//            {
//                if(statsEntry.handleLine(line) && currentSockets.containsKey(statsEntry.getSocketIdentifier()))
//                {
//                    final UdpBufferStats existing = currentSockets.get(statsEntry.getSocketIdentifier());
//                    final UdpBufferStats collected = statsEntry.getUdpBufferStats();
//                    existing.update(collected.receiveQueueDepth, collected.drops);
//                    handler.onStatisticsUpdated(
//                            existing.socketAddress,
//                            statsEntry.getSocketIdentifier(),
//                            existing.receiveQueueDepth,
//                            existing.drops);
//                }
//            }
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }


    public void beginMonitoringOf(final InetSocketAddress socketAddress)
    {
        final Map<Long, UdpBufferStats> currentSockets = mapReference.get();
        final long socketIdentifier = toIdentifier(socketAddress);

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
        final long socketIdentifier = toIdentifier(socketAddress);

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

    private long toIdentifier(final InetSocketAddress socketAddress)
    {
        validateAddressType(socketAddress);
        final int ipAddressOctets = socketAddress.getAddress().hashCode();
        final long port = socketAddress.getPort();
        return port << 32 | ipAddressOctets;
    }

    private static void validateAddressType(final InetSocketAddress socketAddress)
    {
        if(!(socketAddress.getAddress() instanceof Inet4Address))
        {
            throw new IllegalArgumentException("Due to the nature of some awful hacks, " +
                    "I only work with Inet4Address-based sockets");
        }
    }

    private void parse(final String line, final UdpBufferStats udpBufferStats)
    {
        final String[] tokens = line.trim().split("\\s+");
        final String[] txRxQueue = tokens[4].split(":");
        final long receiveQueueDepth = Long.parseLong(txRxQueue[1], 16);
        final long drops = Long.parseLong(tokens[12]);
        udpBufferStats.update(receiveQueueDepth, drops);
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

    private static String calculateBufferIdentifier(final byte[] addressOctets, final int port)
    {
        return
                leftPadTo(2, toHexString(toUnsignedInt(addressOctets[3])).toUpperCase()) +
                        leftPadTo(2, toHexString(toUnsignedInt(addressOctets[2])).toUpperCase()) +
                        leftPadTo(2, toHexString(toUnsignedInt(addressOctets[1])).toUpperCase()) +
                        leftPadTo(2, toHexString(toUnsignedInt(addressOctets[0])).toUpperCase()) +
                        ":" +
                        leftPadTo(4, toHexString(port).toUpperCase());

    }

    private static String leftPadTo(final int totalLength, final String source)
    {
        String result = source;
        for(int i = 0; i < totalLength - source.length(); i++)
        {
            result = "0" + result;
        }

        return result;
    }
}