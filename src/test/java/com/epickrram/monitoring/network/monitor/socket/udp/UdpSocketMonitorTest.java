package com.epickrram.monitoring.network.monitor.socket.udp;

import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class UdpSocketMonitorTest
{
    private final UdpSocketStatisticsHandler recordingUdpSocketStatisticsHandler = new RecordingUdpSocketStatisticsHandler();
    private Path inputPath;
    private UdpSocketMonitor monitor;

    @Before
    public void before() throws Exception
    {
        inputPath = Files.createTempFile("proc-net-udp", "txt");
        copy(currentThread().getContextClassLoader().getResourceAsStream("proc_net_udp_sample.txt"), inputPath, REPLACE_EXISTING);
        monitor = new UdpSocketMonitor(new RecordingUdpSocketMonitoringLifecycleListener(), inputPath);
    }

    @Test
    public void shouldSampleMonitoredSockets() throws Exception
    {


        monitor.poll(recordingUdpSocketStatisticsHandler);


    }

    private static class RecordingUdpSocketMonitoringLifecycleListener implements UdpSocketMonitoringLifecycleListener
    {
        @Override
        public void socketMonitoringStarted(final InetSocketAddress socketAddress)
        {

        }

        @Override
        public void socketMonitoringStopped(final InetSocketAddress socketAddress)
        {

        }
    }

    private static class RecordingUdpSocketStatisticsHandler implements UdpSocketStatisticsHandler
    {
        private final List<MonitoredEntry> recordedEntries = new ArrayList<>();

        @Override
        public void onStatisticsUpdated(final InetSocketAddress socketAddress,
                                        final long socketIdentifier,
                                        final long receiveQueueDepth,
                                        final long drops)
        {
            recordedEntries.add(new MonitoredEntry(socketAddress, socketIdentifier, receiveQueueDepth, drops));
        }

        public List<MonitoredEntry> getRecordedEntries()
        {
            return recordedEntries;
        }
    }

    private static final class MonitoredEntry
    {
        private final InetSocketAddress socketAddress;
        private final long socketIdentifier;
        private final long receiverQueueDepth;
        private final long drops;

        public MonitoredEntry(
                final InetSocketAddress socketAddress,
                final long socketIdentifier,
                final long receiverQueueDepth,
                final long drops)
        {
            this.socketAddress = socketAddress;
            this.socketIdentifier = socketIdentifier;
            this.receiverQueueDepth = receiverQueueDepth;
            this.drops = drops;
        }

        public InetSocketAddress getSocketAddress()
        {
            return socketAddress;
        }

        public long getSocketIdentifier()
        {
            return socketIdentifier;
        }

        public long getReceiverQueueDepth()
        {
            return receiverQueueDepth;
        }

        public long getDrops()
        {
            return drops;
        }
    }
}