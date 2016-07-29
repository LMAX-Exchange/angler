package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public final class RecordingSocketMonitoringLifecycleListener implements SocketMonitoringLifecycleListener
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

    public List<InetSocketAddress> getMonitoringStartedList()
    {
        return monitoringStartedList;
    }

    public List<InetSocketAddress> getMonitoringStoppedList()
    {
        return monitoringStoppedList;
    }
}
