package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

final class RecordingTcpSocketStatisticsHandler implements TcpSocketStatisticsHandler
{
    private final List<MonitoredEntry> recordedEntries = new ArrayList<>();

    @Override
    public void onStatisticsUpdated(final InetAddress inetAddress,
                                    final int port,
                                    final long socketIdentifier,
                                    final long inode,
                                    final long receiveQueueDepth,
                                    final long transmitQueueDepth)
    {
        recordedEntries.add(new MonitoredEntry(inetAddress, port, socketIdentifier, inode,
                receiveQueueDepth, transmitQueueDepth));
    }

    List<MonitoredEntry> getRecordedEntries()
    {
        return recordedEntries;
    }
}
