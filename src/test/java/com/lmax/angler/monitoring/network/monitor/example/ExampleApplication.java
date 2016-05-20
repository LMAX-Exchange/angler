package com.lmax.angler.monitoring.network.monitor.example;

import com.lmax.angler.monitoring.network.monitor.socket.udp.UdpSocketMonitor;
import com.lmax.angler.monitoring.network.monitor.socket.udp.UdpSocketMonitoringLifecycleListener;
import com.lmax.angler.monitoring.network.monitor.socket.udp.UdpSocketStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.system.snmp.SystemNetworkManagementMonitor;
import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.system.softnet.SoftnetStatsHandler;
import com.lmax.angler.monitoring.network.monitor.system.softnet.SoftnetStatsMonitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ExampleApplication implements UdpSocketMonitoringLifecycleListener
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UdpSocketMonitor udpSocketMonitor = new UdpSocketMonitor(this);
    private final SoftnetStatsMonitor softnetStatsMonitor = new SoftnetStatsMonitor();
    private final SystemNetworkManagementMonitor systemNetworkManagementMonitor = new SystemNetworkManagementMonitor();

    private final SoftnetStatsHandler changeLoggingSoftnetStatsHandler = new ChangeLoggingSoftnetStatsHandler();
    private final UdpSocketStatisticsHandler changeLoggingUdpSocketStatisticsHandler = new LoggingUdpSocketStatisticsHandler();
    private final SnmpUdpStatisticsHandler changeLoggingSnmpUdpStatisticsHandler = new ChangeLoggingSnmpUdpStatisticsHandler();

    private void run() throws Exception
    {
        try(final DatagramChannel c0 = createListeningChannelOnPort(new InetSocketAddress(InetAddress.getLocalHost(), 32769));
            final DatagramChannel c1 = createListeningChannelOnPort(new InetSocketAddress(InetAddress.getLocalHost(), 32770)))
        {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::pollMonitors, 0L, 1L, TimeUnit.SECONDS);
            udpSocketMonitor.beginMonitoringOf((InetSocketAddress) c0.getLocalAddress());
            udpSocketMonitor.beginMonitoringOf((InetSocketAddress) c1.getLocalAddress());

            Thread.sleep(TimeUnit.SECONDS.toMillis(1L));

            final DatagramChannel writer = DatagramChannel.open().connect(c0.getLocalAddress());

            while(!Thread.currentThread().isInterrupted())
            {
                // overflow receive buffers
                writer.write(ByteBuffer.wrap("deadcod".getBytes(StandardCharsets.UTF_8)));
                Thread.sleep(200L);
            }
        }
    }

    private void pollMonitors()
    {
        try
        {
            udpSocketMonitor.poll(changeLoggingUdpSocketStatisticsHandler);
            softnetStatsMonitor.poll(changeLoggingSoftnetStatsHandler);
            systemNetworkManagementMonitor.poll(changeLoggingSnmpUdpStatisticsHandler);
            System.out.println("");
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) throws Exception
    {
        new ExampleApplication().run();
    }

    @Override
    public void socketMonitoringStarted(final InetSocketAddress socketAddress, final long inode)
    {
        log("Started monitoring of socket: %s [inode=%d]", socketAddress.toString(), inode);
    }

    @Override
    public void socketMonitoringStopped(final InetSocketAddress socketAddress, final long inode)
    {
        log("Stopped monitoring of socket: %s [inode=%d]", socketAddress.toString(), inode);
    }

    private static void log(final String format, final Object... args)
    {
        System.out.printf("%s %s%n", FORMATTER.format(LocalDateTime.now()), String.format(format, args));
    }

    private static class ChangeLoggingSoftnetStatsHandler implements SoftnetStatsHandler
    {
        private final long[] perCpuDroppedCounts = new long[Runtime.getRuntime().availableProcessors()];

        @Override
        public void perCpuStatistics(final int cpuId, final long processed, final long squeezed, final long dropped)
        {
            if(perCpuDroppedCounts[cpuId] != 0 && perCpuDroppedCounts[cpuId] != dropped)
            {
                log("Softnet stats, cpu: %d, processed: %d, squeezed: %d, dropped: %d", cpuId, processed, squeezed, dropped);
            }
            perCpuDroppedCounts[cpuId] = dropped;
        }
    }

    private static class LoggingUdpSocketStatisticsHandler implements UdpSocketStatisticsHandler
    {
        @Override
        public void onStatisticsUpdated(final InetSocketAddress socketAddress, final int port, final long socketIdentifier,
                                        final long inode, final long receiveQueueDepth, final long drops)
        {
            log("Socket [%s], queued: %d, drops: %d", socketAddress.toString(), receiveQueueDepth, drops);
        }
    }

    private static class ChangeLoggingSnmpUdpStatisticsHandler implements SnmpUdpStatisticsHandler
    {
        private long lastReceiveBufferErrors = -1L;

        @Override
        public void onStatisticsUpdated(final long inErrors, final long receiveBufferErrors, final long inChecksumErrors)
        {
            if(receiveBufferErrors != lastReceiveBufferErrors)
            {
                log("UDP stats, inErrors: %d, recvBufErrors: %d, inChksumErrors: %d", inErrors, receiveBufferErrors, inChecksumErrors);
            }
            lastReceiveBufferErrors = receiveBufferErrors;
        }
    }

    private static DatagramChannel createListeningChannelOnPort(final InetSocketAddress local) throws IOException
    {
        return DatagramChannel.open().
                setOption(StandardSocketOptions.SO_REUSEADDR, true).
                setOption(StandardSocketOptions.SO_RCVBUF, 4096).
                bind(local);
    }
}
