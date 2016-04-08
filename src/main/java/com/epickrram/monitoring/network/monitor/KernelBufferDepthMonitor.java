package com.epickrram.monitoring.network.monitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Integer.toHexString;
import static java.nio.file.Files.readAllLines;

public final class KernelBufferDepthMonitor
{
    private static final Path UDP_BUFFER_STATS_FILE = Paths.get("/proc/net/udp");

    private final String bufferIdentifier;
    private final UdpBufferStats udpBufferStats = new UdpBufferStats(0L, 0L);

    public KernelBufferDepthMonitor(
            final SocketAddress address)
    {
        final InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
        final InetAddress inetAddress = inetSocketAddress.getAddress();
        final byte[] addressOctets = inetAddress.getAddress();
        final int port = inetSocketAddress.getPort();

        this.bufferIdentifier = calculateBufferIdentifier(addressOctets, port);
    }

    public void report()
    {
        try
        {
            readAllLines(UDP_BUFFER_STATS_FILE).stream().
                filter(l -> l.contains(bufferIdentifier)).
                forEach(this::parse);

            if(udpBufferStats.changed)
            {
                System.out.printf("Drops: %d, depth: %d%n",
                        udpBufferStats.drops,
                        udpBufferStats.receiveQueueDepth);
            }
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void parse(final String line)
    {
        final String[] tokens = line.split("\\s+");
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

        public UdpBufferStats(final long receiveQueueDepth, final long drops)
        {
            this.receiveQueueDepth = receiveQueueDepth;
            this.drops = drops;
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
                leftPadTo(2, toHexString(addressOctets[3]).toUpperCase()) +
                leftPadTo(2, toHexString(addressOctets[2]).toUpperCase()) +
                leftPadTo(2, toHexString(addressOctets[1]).toUpperCase()) +
                leftPadTo(2, toHexString(addressOctets[0]).toUpperCase()) +
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
