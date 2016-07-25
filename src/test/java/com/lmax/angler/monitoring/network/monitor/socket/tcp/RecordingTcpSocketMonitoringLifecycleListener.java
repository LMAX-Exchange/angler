package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

final class RecordingTcpSocketMonitoringLifecycleListener implements TcpSocketMonitoringLifecycleListener
{
    private final List<InetSocketAddress> monitoringStartedList = new ArrayList<>();
    private final List<InetSocketAddress> monitoringStoppedList = new ArrayList<>();

    @Override
    public void socketMonitoringStarted(final InetAddress inetAddress, final int port, final long inode)
    {
        monitoringStartedList.add(new InetSocketAddress(inetAddress, port));
    }

    @Override
    public void socketMonitoringStopped(final InetAddress inetAddress, final int port, final long inode)
    {
        monitoringStoppedList.add(new InetSocketAddress(inetAddress, port));
    }

    List<InetSocketAddress> getMonitoringStartedList()
    {
        return monitoringStartedList;
    }

    List<InetSocketAddress> getMonitoringStoppedList()
    {
        return monitoringStoppedList;
    }
}
