package com.lmax.angler.monitoring.network.monitor.system.snmp;

import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.lmax.angler.monitoring.network.monitor.ResourceUtil.writeDataFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SystemNetworkManagementMonitorTest
{
    private final RecordingSnmpUdpStatisticsHandler statsHandler = new RecordingSnmpUdpStatisticsHandler();
    private Path inputPath;
    private SystemNetworkManagementMonitor monitor;

    @Before
    public void before() throws Exception
    {
        inputPath = Files.createTempFile("proc-net-snmp", "txt");
        writeDataFile("proc_net_snmp_sample.txt", inputPath);
        monitor = new SystemNetworkManagementMonitor(inputPath);
    }

    @After
    public void after() throws Exception
    {
        Files.deleteIfExists(inputPath);
    }

    @Test
    public void shouldParseDataAndNotifyListener() throws Exception
    {
        monitor.poll(statsHandler);

        assertThat(statsHandler.getRecorded().size(), is(1));
        assertEntry(statsHandler.getRecorded().get(0), 78665L, 62374L, 37L);
    }

    private static void assertEntry(
            final SnmpUdpStatistic snmpUdpStatistic,
            final long inErrors,
            final long receiveBufferErrors,
            final long inChecksumErrors)
    {
        assertThat(snmpUdpStatistic.getInErrors(), is(inErrors));
        assertThat(snmpUdpStatistic.getReceiveBufferErrors(), is(receiveBufferErrors));
        assertThat(snmpUdpStatistic.getInChecksumErrors(), is(inChecksumErrors));
    }

    private static final class RecordingSnmpUdpStatisticsHandler implements SnmpUdpStatisticsHandler
    {
        private final List<SnmpUdpStatistic> recorded = new ArrayList<>();

        @Override
        public void onStatisticsUpdated(final long inErrors,
                                        final long receiveBufferErrors,
                                        final long inChecksumErrors)
        {
            recorded.add(new SnmpUdpStatistic(inErrors, receiveBufferErrors, inChecksumErrors));
        }

        List<SnmpUdpStatistic> getRecorded()
        {
            return recorded;
        }
    }

    private static final class SnmpUdpStatistic
    {
        private final long inErrors;
        private final long receiveBufferErrors;
        private final long inChecksumErrors;


        SnmpUdpStatistic(final long inErrors, final long receiveBufferErrors, final long inChecksumErrors)
        {
            this.inErrors = inErrors;
            this.receiveBufferErrors = receiveBufferErrors;
            this.inChecksumErrors = inChecksumErrors;
        }

        long getInErrors()
        {
            return inErrors;
        }

        long getReceiveBufferErrors()
        {
            return receiveBufferErrors;
        }

        long getInChecksumErrors()
        {
            return inChecksumErrors;
        }
    }

}