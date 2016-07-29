package com.lmax.angler.monitoring.network.monitor.example;

import com.lmax.angler.monitoring.network.monitor.socket.SocketMonitoringLifecycleListener;
import com.lmax.angler.monitoring.network.monitor.socket.udp.UdpSocketMonitor;
import com.lmax.angler.monitoring.network.monitor.socket.udp.UdpSocketStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.system.snmp.SystemNetworkManagementMonitor;
import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.system.softnet.SoftnetStatsHandler;
import com.lmax.angler.monitoring.network.monitor.system.softnet.SoftnetStatsMonitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExampleApplication implements SocketMonitoringLifecycleListener
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UdpSocketMonitor udpSocketMonitor = new UdpSocketMonitor(this);
    private final SoftnetStatsMonitor softnetStatsMonitor = new SoftnetStatsMonitor();
    private final SystemNetworkManagementMonitor systemNetworkManagementMonitor = new SystemNetworkManagementMonitor();

    private final SoftnetStatsHandler changeLoggingSoftnetStatsHandler = new ChangeLoggingSoftnetStatsHandler();
    private final UdpSocketStatisticsHandler changeLoggingUdpSocketStatisticsHandler = new LoggingUdpSocketStatisticsHandler();
    private final SnmpUdpStatisticsHandler changeLoggingSnmpUdpStatisticsHandler = new ChangeLoggingSnmpUdpStatisticsHandler();
    private final AtomicInteger monitoredSocketCount = new AtomicInteger(0);

    private void run() throws Exception
    {
        try(final DatagramChannel c0 = createListeningChannelOnPort(new InetSocketAddress(InetAddress.getLocalHost(), 32769));
            final DatagramChannel c1 = createListeningChannelOnPort(new InetSocketAddress(InetAddress.getLocalHost(), 32770));
            final DatagramChannel c2 = multicastListener(InetAddress.getByName("239.192.45.3"), 5000))
        {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::pollMonitors, 0L, 1L, TimeUnit.SECONDS);
            udpSocketMonitor.beginMonitoringOf((InetSocketAddress) c0.getLocalAddress());
            udpSocketMonitor.beginMonitoringOf((InetSocketAddress) c1.getLocalAddress());
            udpSocketMonitor.beginMonitoringOf((InetSocketAddress) c2.getLocalAddress());

            waitForMonitoringToStart();

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

    @Override
    public void socketMonitoringStarted(final InetAddress inetAddress, final int port, final long inode)
    {
        log("Started monitoring of socket: %s:%d [inode=%d]", inetAddress.toString(), port, inode);
        monitoredSocketCount.getAndIncrement();
    }

    @Override
    public void socketMonitoringStopped(final InetAddress inetAddress, final int port, final long inode)
    {
        log("Stopped monitoring of socket: %s:%d [inode=%d]", inetAddress.toString(), port, inode);
    }

    private void waitForMonitoringToStart() throws InterruptedException
    {
        final long timeoutAt = System.currentTimeMillis() + 5000L;
        while(!Thread.currentThread().isInterrupted() &&
                System.currentTimeMillis() < timeoutAt &&
                monitoredSocketCount.get() < 3)
        {
            Thread.sleep(100L);
        }
        if(monitoredSocketCount.get() < 3)
        {
            throw new IllegalStateException("Sockets did not become available for monitoring!");
        }
    }

    public static void main(final String[] args) throws Exception
    {
        new ExampleApplication().run();
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
        public void onStatisticsUpdated(final InetAddress socketAddress, final int port, final long socketIdentifier,
                                        final long inode, final long receiveQueueDepth, final long transmitQueueDepth,
                                        final long drops)
        {
            log("Socket [%s:%d], queued: %d, drops: %d", socketAddress.toString(), port, receiveQueueDepth, drops);
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

    private static DatagramChannel multicastListener(final InetAddress address, final int port) throws IOException
    {
        final NetworkInterface networkInterface = getMulticastCapableNetworkInterface();
        final DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.bind(new InetSocketAddress(address, port));
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        channel.join(address, networkInterface);

        return channel;
    }

    private static DatagramChannel createListeningChannelOnPort(final InetSocketAddress local) throws IOException
    {
        return DatagramChannel.open(StandardProtocolFamily.INET).
                setOption(StandardSocketOptions.SO_REUSEADDR, true).
                setOption(StandardSocketOptions.SO_RCVBUF, 4096).
                bind(local);
    }

    private static NetworkInterface getMulticastCapableNetworkInterface() throws SocketException
    {
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while(networkInterfaces.hasMoreElements())
        {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            if(networkInterface.supportsMulticast())
            {
                return networkInterface;
            }
        }

        throw new IllegalStateException("Unable to find multicast-capable interface");
    }
}
