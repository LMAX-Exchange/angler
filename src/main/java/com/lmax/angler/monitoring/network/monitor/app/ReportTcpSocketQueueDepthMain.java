package com.lmax.angler.monitoring.network.monitor.app;

import com.lmax.angler.monitoring.network.monitor.socket.SocketMonitoringLifecycleListener;
import com.lmax.angler.monitoring.network.monitor.socket.tcp.TcpSocketMonitor;
import com.lmax.angler.monitoring.network.monitor.socket.tcp.TcpSocketStatisticsHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public final class ReportTcpSocketQueueDepthMain implements
        SocketMonitoringLifecycleListener, TcpSocketStatisticsHandler, Runnable
{
    private static final long SAMPLE_INTERVAL_MS = Long.getLong("angler.tcp.sample.ms", 1000);
    private final TcpSocketMonitor tcpSocketMonitor;
    private final Map<InetSocketAddress, Long> rxMaxima = new HashMap<>();
    private final Map<InetSocketAddress, Long> txMaxima = new HashMap<>();
    private long nextReportTimestamp;

    public static void main(String[] args) throws UnknownHostException
    {
        if (args.length == 0)
        {
            throw new IllegalStateException("Supply an address to monitor (e.g 10.0.7.24)");
        }
        new ReportTcpSocketQueueDepthMain(InetAddress.getByName(args[0])).start();
    }

    private ReportTcpSocketQueueDepthMain(final InetAddress addressToMonitor)
    {
        tcpSocketMonitor = new TcpSocketMonitor(this);
        tcpSocketMonitor.beginMonitoringOf(addressToMonitor);
    }

    private void start()
    {
        Executors.newSingleThreadScheduledExecutor().
                scheduleAtFixedRate(this, SAMPLE_INTERVAL_MS, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run()
    {
        tcpSocketMonitor.poll(this);
    }

    @Override
    public void onStatisticsUpdated(final InetAddress inetAddress, final int port,
                                    final long socketIdentifier, final long inode,
                                    final long receiveQueueDepth, final long transmitQueueDepth)
    {
        final InetSocketAddress key = new InetSocketAddress(inetAddress, port);
        updateMaximum(receiveQueueDepth, rxMaxima, key);
        updateMaximum(transmitQueueDepth, txMaxima, key);

        if (System.currentTimeMillis() > nextReportTimestamp)
        {
            final long epochSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            for (final InetSocketAddress socket : rxMaxima.keySet())
            {
                System.out.printf("%d %s; RX: %s, TX: %s%n", epochSeconds,
                  socket, rxMaxima.get(socket), txMaxima.get(socket));
            }
            nextReportTimestamp = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1L);
            rxMaxima.clear();
            txMaxima.clear();
        }
    }

    private void updateMaximum(final long value, final Map<InetSocketAddress, Long> maxima, final InetSocketAddress key)
    {
        Long existing = maxima.get(key);
        if (existing == null)
        {
            existing = 0L;
        }
        maxima.put(key, Math.max(existing, value));
    }

    @Override
    public void socketMonitoringStarted(final InetAddress inetAddress, final int port, final long inode)
    {
        System.out.printf("Started monitoring %s:%d%n", inetAddress, port);
    }

    @Override
    public void socketMonitoringStopped(InetAddress inetAddress, int port, long inode)
    {
        // no-op
    }
}