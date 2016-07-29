package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AddressOnlySpecTcpSocketMonitorTest extends TcpSocketMonitorTest<InetAddress>
{
    @Override
    protected Consumer<InetAddress> getBeginMonitoringRequestMethod()
    {
        return monitor::beginMonitoringOf;
    }

    @Override
    protected Consumer<InetAddress> getEndMonitoringRequestMethod()
    {
        return monitor::endMonitoringOf;
    }

    @Override
    protected Collection<InetAddress> requestSpecFor(final InetSocketAddress... request)
    {
        return
                Arrays.stream(request).map(s -> s.getAddress().getHostAddress()).
                        collect(Collectors.toSet()).stream().map(this::getByNameOrThrow).
                        collect(Collectors.toList());
    }

    @Test
    public void shouldNotifyLifecycleListener() throws Exception
    {
        beginMonitoring(requestSpecFor(
                getSocketAddress("0.0.0.0", 20048),
                getSocketAddress("0.0.0.0", 56150)));

        monitor.poll(recordingTcpSocketStatisticsHandler);

        endMonitoring(requestSpecFor(getSocketAddress("0.0.0.0", 56150)));

        monitor.poll(recordingTcpSocketStatisticsHandler);

        final List<InetSocketAddress> monitoringStartedList = lifecycleListener.getMonitoringStartedList();
        assertThat(monitoringStartedList.size(), is(2));
        assertThat(monitoringStartedList.get(0), is(getSocketAddress("0.0.0.0", 20048)));
        assertThat(monitoringStartedList.get(1), is(getSocketAddress("0.0.0.0", 56150)));

        final List<InetSocketAddress> monitoringStoppedList = lifecycleListener.getMonitoringStoppedList();
        assertThat(monitoringStoppedList.size(), is(2));
        assertThat(monitoringStoppedList.get(0), is(getSocketAddress("0.0.0.0", 56150)));
        assertThat(monitoringStoppedList.get(1), is(getSocketAddress("0.0.0.0", 20048)));
    }

    private InetAddress getByNameOrThrow(final String host)
    {
        try
        {
            return InetAddress.getByName(host);
        }
        catch (UnknownHostException e)
        {
            throw new IllegalArgumentException("Cannot create InetAddress from host: " + host);
        }
    }
}