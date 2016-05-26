package com.lmax.angler.monitoring.network.monitor.socket.udp;

import com.lmax.angler.monitoring.network.monitor.ResourceUtil;
import com.lmax.angler.monitoring.network.monitor.socket.SocketIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class UdpSocketMonitorTest<T>
{
    protected final RecordingUdpSocketStatisticsHandler recordingUdpSocketStatisticsHandler =
            new RecordingUdpSocketStatisticsHandler();
    protected final RecordingUdpSocketMonitoringLifecycleListener lifecycleListener =
            new RecordingUdpSocketMonitoringLifecycleListener();
    private Path inputPath;
    protected UdpSocketMonitor monitor;

    protected abstract Consumer<T> getBeginMonitoringRequestMethod();
    protected abstract Consumer<T> getEndMonitoringRequestMethod();
    protected abstract Collection<T> requestSpecFor(final InetSocketAddress... request);

    @Before
    public void before() throws Exception
    {
        inputPath = Files.createTempFile("proc-net-udp", "txt");
        ResourceUtil.writeDataFile("proc_net_udp_sample.txt", inputPath);
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
        beginMonitoring(requestSpecFor(
                getSocketAddress("0.0.0.0", 20048),
                getSocketAddress("0.0.0.0", 56150),
                getSocketAddress("192.168.122.1", 53)));

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
        beginMonitoring(requestSpecFor(
                getSocketAddress("0.0.0.0", 20048),
                getSocketAddress("0.0.0.0", 56150),
                getSocketAddress("192.168.122.1", 53)));

        monitor.poll(recordingUdpSocketStatisticsHandler);
        recordingUdpSocketStatisticsHandler.getRecordedEntries().clear();

        ResourceUtil.writeDataFile("proc_net_udp_updated_sample.txt", inputPath);

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(2));
        assertEntry(recordedEntries.get(0), "0.0.0.0", 56150, 1, 4, 13597);
        assertEntry(recordedEntries.get(1), "192.168.122.1", 53, 166, 2, 15292);
    }

    @Test
    public void shouldNotNotifyHandlerOfChangeWhenSocketIsNoLongerMonitored() throws Exception
    {
        beginMonitoring(requestSpecFor(
                getSocketAddress("0.0.0.0", 20048),
                getSocketAddress("0.0.0.0", 56150),
                getSocketAddress("192.168.122.1", 53)));

        monitor.poll(recordingUdpSocketStatisticsHandler);
        recordingUdpSocketStatisticsHandler.getRecordedEntries().clear();

        ResourceUtil.writeDataFile("proc_net_udp_updated_sample.txt", inputPath);

        endMonitoring(requestSpecFor(getSocketAddress("192.168.122.1", 53)));

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(1));
        assertEntry(recordedEntries.get(0), "0.0.0.0", 56150, 1, 4, 13597);
    }

    @Test
    public void shouldNotifyLifecycleListenerWhenMonitoredSocketBecomesUnavailable() throws Exception
    {
        beginMonitoring(requestSpecFor(getSocketAddress("0.0.0.0", 20048)));
        monitor.poll(recordingUdpSocketStatisticsHandler);

        ResourceUtil.writeDataFile("proc_net_udp_socket_removed_sample.txt", inputPath);

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<InetSocketAddress> monitoringStoppedList = lifecycleListener.getMonitoringStoppedList();
        assertThat(monitoringStoppedList.size(), is(1));
        assertThat(monitoringStoppedList.get(0), is(getSocketAddress("0.0.0.0", 20048)));
    }

    @Test
    public void shouldMonitorIpAddressWhoseFirstOctetJavaSignedByteValueIsNegative() throws Exception
    {
        beginMonitoring(requestSpecFor(getSocketAddress("239.168.122.1", 53)));
        monitor.poll(recordingUdpSocketStatisticsHandler);

        ResourceUtil.writeDataFile("proc_net_udp_signed_first_octet_sample.txt", inputPath);

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(1));
        assertEntry(recordedEntries.get(0), "239.168.122.1", 53, 166, 0, 15292);
    }

    @Test
    public void shouldMonitorIpAddressOnLocalhost() throws Exception
    {
        beginMonitoring(requestSpecFor(getSocketAddress("127.0.0.1", 32770)));
        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(1));
        assertEntry(recordedEntries.get(0), "127.0.0.1", 32770, 0, 0, 15293);
    }

    private static void assertEntry(final MonitoredEntry monitoredEntry,
                                    final String address,
                                    final int port,
                                    final long queueDepth,
                                    final long dropCount,
                                    final long inode) throws UnknownHostException
    {
        final long socketIdentifier = monitoredEntry.getSocketIdentifier();
        assertThat(extractHostIpAddress(socketIdentifier), is(address));
        assertThat(SocketIdentifier.extractPortNumber(socketIdentifier), is(port));
        assertThat(monitoredEntry.getReceiverQueueDepth(), is(queueDepth));
        assertThat(monitoredEntry.getDrops(), is(dropCount));
        assertThat(monitoredEntry.getInode(), is(inode));
        assertThat(monitoredEntry.getSocketAddress(), is(new InetSocketAddress(address, port)));
    }

    InetSocketAddress getSocketAddress(final String host, final int port) throws UnknownHostException
    {
        return new InetSocketAddress(InetAddress.getByName(host), port);
    }

    void beginMonitoring(final Collection<T> requestSpec)
    {
        requestSpec.stream().forEach(getBeginMonitoringRequestMethod());
    }

    void endMonitoring(final Collection<T> requestSpec)
    {
        requestSpec.stream().forEach(getEndMonitoringRequestMethod());
    }

    private static String extractHostIpAddress(final long socketIdentifier) throws UnknownHostException
    {
        final byte[] address = new byte[4];
        address[3] = (byte) (socketIdentifier & 0xFF);
        address[2] = (byte) (socketIdentifier >> 8 & 0xFF);
        address[1] = (byte) (socketIdentifier >> 16 & 0xFF);
        address[0] = (byte) (socketIdentifier >> 24 & 0xFF);
        return Inet4Address.getByAddress(address).getHostAddress();
    }
}