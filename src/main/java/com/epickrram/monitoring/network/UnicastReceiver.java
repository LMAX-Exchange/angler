package com.epickrram.monitoring.network;

import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.lang.Thread.currentThread;

public final class UnicastReceiver
{
    private static final int UNSET = -1;
    private static final int MTU_SIZE = 1500;

    private final SocketAddress address;
    private volatile int receiveBufferSize = UNSET;

    public UnicastReceiver(
            final SocketAddress address)
    {
        this.address = address;
    }

    public void receiveLoop()
    {
        final Histogram packetSizeHistogram = new Histogram(MTU_SIZE, 4);
        final Histogram latencyHistogram = new Histogram(TimeUnit.MILLISECONDS.toNanos(5L), 3);

        try
        {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(MTU_SIZE);
            final DatagramChannel channel = DatagramChannel.open();
            channel.setOption(StandardSocketOptions.SO_RCVBUF, 4096);
            this.receiveBufferSize = channel.getOption(StandardSocketOptions.SO_RCVBUF);
            channel.bind(address);
            channel.configureBlocking(true);
            while(!currentThread().isInterrupted())
            {
                buffer.clear();
                channel.receive(buffer);
                final long receivedNanos = System.nanoTime();

                buffer.flip();
                final long transmitLatency = receivedNanos - buffer.getLong();

                packetSizeHistogram.recordValue(buffer.remaining());
                latencyHistogram.recordValue(Math.min(latencyHistogram.getHighestTrackableValue(), transmitLatency));
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            System.out.printf("Received packet size histogram%n%n");
            packetSizeHistogram.outputPercentileDistribution(System.out, 1d);

            System.out.printf("%n%nTransmit latency histogram%n%n");
            latencyHistogram.outputPercentileDistribution(System.out, 1d);
        }
    }

    public int getConfiguredReceiveBufferSize()
    {
        while(receiveBufferSize == UNSET)
        {
            LockSupport.parkNanos(1);
        }

        return receiveBufferSize;
    }
}