package com.epickrram.monitoring.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

import static java.lang.Thread.currentThread;

public final class UnicastSender
{
    private final SocketAddress address;
    private final LongUnaryOperator publicationIntervalCalculator;
    private final Supplier<ByteBuffer> messageFactory;

    public UnicastSender(
            final SocketAddress address,
            final LongUnaryOperator publicationIntervalCalculator,
            final Supplier<ByteBuffer> messageFactory)
    {
        this.address = address;
        this.publicationIntervalCalculator = publicationIntervalCalculator;
        this.messageFactory = messageFactory;
    }

    public void sendLoop()
    {
        try
        {
            final DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(true);
            final long startNanoSeconds = System.nanoTime();
            while(!currentThread().isInterrupted())
            {
                final ByteBuffer message = messageFactory.get();
                channel.send(message, address);
                LockSupport.parkNanos(publicationIntervalCalculator.applyAsLong(System.nanoTime() - startNanoSeconds));
            }
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
