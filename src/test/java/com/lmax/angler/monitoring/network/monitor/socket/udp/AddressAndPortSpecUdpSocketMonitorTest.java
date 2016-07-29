package com.lmax.angler.monitoring.network.monitor.socket.udp;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AddressAndPortSpecUdpSocketMonitorTest extends UdpSocketMonitorTest<InetSocketAddress>
{
    @Override
    protected Consumer<InetSocketAddress> getBeginMonitoringRequestMethod()
    {
        return monitor::beginMonitoringOf;
    }

    @Override
    protected Consumer<InetSocketAddress> getEndMonitoringRequestMethod()
    {
        return monitor::endMonitoringOf;
    }

    @Override
    protected Collection<InetSocketAddress> requestSpecFor(final InetSocketAddress... request)
    {
        return Arrays.stream(request).collect(Collectors.toList());
    }

    @Test
    public void shouldNotifyLifecycleListener() throws Exception
    {
        beginMonitoring(requestSpecFor(
                getSocketAddress("0.0.0.0", 20048),
                getSocketAddress("0.0.0.0", 56150)));

        monitor.poll(recordingUdpSocketStatisticsHandler);

        endMonitoring(requestSpecFor(getSocketAddress("0.0.0.0", 56150)));

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
    public void shouldBeAbleToSubscribeToAddressAndPortAndInode() throws Exception
    {
        monitor.beginMonitoringOf(getSocketAddress("192.168.122.2", 53), 15294);

        monitor.poll(recordingUdpSocketStatisticsHandler);

        final List<MonitoredEntry> recordedEntries = recordingUdpSocketStatisticsHandler.getRecordedEntries();
        assertThat(recordedEntries.size(), is(1));
        assertEntry(recordedEntries.get(0), "192.168.122.2", 53, 9, 0, 0, 15294);
    }
}