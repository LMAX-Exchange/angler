package com.epickrram.monitoring.network;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.epickrram.monitoring.network.SendingRates.constant;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class BufferMonitorMain
{
    public static void main(final String[] args) throws Exception
    {
        final SocketAddress address = new InetSocketAddress(Inet4Address.getLocalHost(), 51000);
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
        final MessageFactory messageFactory = new MessageFactory();

        final UnicastReceiver receiver = new UnicastReceiver(address);
        final UnicastSender sender = new UnicastSender(address, constant(100, MILLISECONDS), messageFactory::prepare);
        final KernelBufferDepthMonitor bufferDepthMonitor = new KernelBufferDepthMonitor(address);
        final SoftIrqHandlerTimeSqueezeMonitor timeSqueezeMonitor = new SoftIrqHandlerTimeSqueezeMonitor();

        executorService.submit(receiver::receiveLoop);
        executorService.submit(sender::sendLoop);

        executorService.scheduleAtFixedRate(() -> {
            bufferDepthMonitor.report();
            timeSqueezeMonitor.report();
        }, 1L, 1L, SECONDS);

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(30L));

        executorService.shutdownNow();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}