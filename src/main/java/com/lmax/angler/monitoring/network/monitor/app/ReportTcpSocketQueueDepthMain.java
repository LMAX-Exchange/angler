package com.lmax.angler.monitoring.network.monitor.app;

import com.lmax.angler.monitoring.network.monitor.socket.SocketMonitoringLifecycleListener;
import com.lmax.angler.monitoring.network.monitor.socket.tcp.TcpSocketMonitor;
import com.lmax.angler.monitoring.network.monitor.socket.tcp.TcpSocketStatisticsHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ReportTcpSocketQueueDepthMain implements
        SocketMonitoringLifecycleListener, TcpSocketStatisticsHandler, Runnable
{
    private final TcpSocketMonitor tcpSocketMonitor;

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
                scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
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
        System.out.printf("%d %s:%d; RX: %d, TX: %d%n", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                inetAddress, port, receiveQueueDepth, transmitQueueDepth);
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