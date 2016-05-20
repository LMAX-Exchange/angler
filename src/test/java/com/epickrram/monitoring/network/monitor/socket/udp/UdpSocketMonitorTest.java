package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.socket.SocketIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.epickrram.monitoring.network.monitor.ResourceUtil.writeDataFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UdpSocketMonitorTest
{
    private final RecordingUdpSocketStatisticsHandler recordingUdpSocketStatisticsHandler =
            new RecordingUdpSocketStatisticsHandler();
    private final RecordingUdpSocketMonitoringLifecycleListener lifecycleListener =
            new RecordingUdpSocketMonitoringLifecycleListener();
    private Path inputPath;
    private UdpSocketMonitor monitor;

    @Before
    public void before() throws Exception
    {
        inputPath = Files.createTempFile("proc-net-udp", "txt");
        writeDataFile("proc_net_udp_sample.txt", inputPath);
        monitor = new UdpSocketMonitor(lifecycleListener, inputPath);
    }

    @After
    public void after() throws Exception
    {
        Files.deleteIfExists(inputPath);
    }

    @Test
    public void shouldSampleMonitoredSockets() throws Exception
    {

        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 20048));
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 56150));
        monitor.beginMonitoringOf(getSocketAddress("192.168.122.1", 53));
        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(3));
        assertEntry(recordedEntries.get(0), "0.0.0.0", 20048, 0, 0, 21682);
        assertEntry(recordedEntries.get(1), "0.0.0.0", 56150, 0, 4, 13597);
        assertEntry(recordedEntries.get(2), "192.168.122.1", 53, 166, 0, 15292);
    }

    @Test
    public void shouldNotNotifyHandlerOfUnchangedEntries() throws Exception
    {
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 20048));
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 56150));
        monitor.beginMonitoringOf(getSocketAddress("192.168.122.1", 53));
        monitor.poll(recordingUdpSocketStatisticsHandler);
        recordingUdpSocketStatisticsHandler.getRecordedEntries().clear();

        writeDataFile("proc_net_udp_updated_sample.txt", inputPath);

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(2));
        assertEntry(recordedEntries.get(0), "0.0.0.0", 56150, 1, 4, 13597);
        assertEntry(recordedEntries.get(1), "192.168.122.1", 53, 166, 2, 15292);
    }

    @Test
    public void shouldNotNotifyHandlerOfChangeWhenSocketIsNoLongerMonitored() throws Exception
    {
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 20048));
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 56150));
        monitor.beginMonitoringOf(getSocketAddress("192.168.122.1", 53));
        monitor.poll(recordingUdpSocketStatisticsHandler);
        recordingUdpSocketStatisticsHandler.getRecordedEntries().clear();

        writeDataFile("proc_net_udp_updated_sample.txt", inputPath);

        monitor.endMonitoringOf(getSocketAddress("192.168.122.1", 53));

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(1));
        assertEntry(recordedEntries.get(0), "0.0.0.0", 56150, 1, 4, 13597);
    }

    @Test
    public void shouldNotifyLifecycleListener() throws Exception
    {
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 20048));
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 56150));

        monitor.poll(recordingUdpSocketStatisticsHandler);

        monitor.endMonitoringOf(getSocketAddress("0.0.0.0", 56150));

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<InetSocketAddress> monitoringStartedList = lifecycleListener.getMonitoringStartedList();
        assertThat(monitoringStartedList.size(), is(2));
        assertThat(monitoringStartedList.get(0), is(getSocketAddress("0.0.0.0", 20048)));
        assertThat(monitoringStartedList.get(1), is(getSocketAddress("0.0.0.0", 56150)));

        final List<InetSocketAddress> monitoringStoppedList = lifecycleListener.getMonitoringStoppedList();
        assertThat(monitoringStoppedList.size(), is(1));
        assertThat(monitoringStoppedList.get(0), is(getSocketAddress("0.0.0.0", 56150)));
    }

    @Test
    public void shouldNotifyLifecycleListenerWhenMonitoredSocketBecomesUnavailable() throws Exception
    {
        monitor.beginMonitoringOf(getSocketAddress("0.0.0.0", 20048));
        monitor.poll(recordingUdpSocketStatisticsHandler);

        writeDataFile("proc_net_udp_socket_removed_sample.txt", inputPath);

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<InetSocketAddress> monitoringStoppedList = lifecycleListener.getMonitoringStoppedList();
        assertThat(monitoringStoppedList.size(), is(1));
        assertThat(monitoringStoppedList.get(0), is(getSocketAddress("0.0.0.0", 20048)));
    }

    private static void assertEntry(final MonitoredEntry monitoredEntry,
                                    final String address,
                                    final int port,
                                    final long queueDepth,
                                    final long dropCount,
                                    final long inode) throws UnknownHostException
    {
        final long socketIdentifier = monitoredEntry.getSocketIdentifier();
        assertThat(SocketIdentifier.extractHostIpAddress(socketIdentifier), is(address));
        assertThat(SocketIdentifier.extractPortNumber(socketIdentifier), is(port));
        assertThat(monitoredEntry.getReceiverQueueDepth(), is(queueDepth));
        assertThat(monitoredEntry.getDrops(), is(dropCount));
        assertThat(monitoredEntry.getInode(), is(inode));
        assertThat(monitoredEntry.getSocketAddress(), is(new InetSocketAddress(address, port)));
    }

    private InetSocketAddress getSocketAddress(final String host, final int port) throws UnknownHostException
    {
        return new InetSocketAddress(InetAddress.getByName(host), port);
    }

    private static class RecordingUdpSocketMonitoringLifecycleListener implements UdpSocketMonitoringLifecycleListener
    {
        private final List<InetSocketAddress> monitoringStartedList = new ArrayList<>();
        private final List<InetSocketAddress> monitoringStoppedList = new ArrayList<>();

        @Override
        public void socketMonitoringStarted(final InetSocketAddress socketAddress, final long inode)
        {
            monitoringStartedList.add(socketAddress);
        }

        @Override
        public void socketMonitoringStopped(final InetSocketAddress socketAddress, final long inode)
        {
            monitoringStoppedList.add(socketAddress);
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

    private static class RecordingUdpSocketStatisticsHandler implements UdpSocketStatisticsHandler
    {
        private final List<MonitoredEntry> recordedEntries = new ArrayList<>();

        @Override
        public void onStatisticsUpdated(final InetSocketAddress socketAddress,
                                        final long socketIdentifier,
                                        final long inode,
                                        final long receiveQueueDepth,
                                        final long drops)
        {
            recordedEntries.add(new MonitoredEntry(socketAddress, socketIdentifier, inode, receiveQueueDepth, drops));
        }

        List<MonitoredEntry> getRecordedEntries()
        {
            return recordedEntries;
        }
    }

    private static final class MonitoredEntry
    {
        private final InetSocketAddress socketAddress;
        private final long socketIdentifier;
        private final long inode;
        private final long receiverQueueDepth;
        private final long drops;

        MonitoredEntry(
                final InetSocketAddress socketAddress,
                final long socketIdentifier,
                final long inode,
                final long receiverQueueDepth,
                final long drops)
        {
            this.socketAddress = socketAddress;
            this.socketIdentifier = socketIdentifier;
            this.inode = inode;
            this.receiverQueueDepth = receiverQueueDepth;
            this.drops = drops;
        }

        InetSocketAddress getSocketAddress()
        {
            return socketAddress;
        }

        long getSocketIdentifier()
        {
            return socketIdentifier;
        }

        long getReceiverQueueDepth()
        {
            return receiverQueueDepth;
        }

        long getDrops()
        {
            return drops;
        }

        long getInode()
        {
            return inode;
        }
    }
}