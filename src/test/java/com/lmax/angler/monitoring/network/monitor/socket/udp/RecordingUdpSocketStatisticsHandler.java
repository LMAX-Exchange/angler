package com.lmax.angler.monitoring.network.monitor.socket.udp;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

final class RecordingUdpSocketStatisticsHandler implements UdpSocketStatisticsHandler
{
    private final List<MonitoredEntry> recordedEntries = new ArrayList<>();

    @Override
    public void onStatisticsUpdated(final InetAddress inetAddress,
                                    final int port,
                                    final long socketIdentifier,
                                    final long inode,
                                    final long receiveQueueDepth,
                                    final long drops)
    {
        recordedEntries.add(new MonitoredEntry(inetAddress, port, socketIdentifier, inode, receiveQueueDepth, drops));
    }

    List<MonitoredEntry> getRecordedEntries()
    {
        return recordedEntries;
    }
}
