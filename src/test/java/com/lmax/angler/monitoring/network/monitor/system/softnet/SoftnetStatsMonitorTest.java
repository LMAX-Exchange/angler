package com.lmax.angler.monitoring.network.monitor.system.softnet;

import com.lmax.angler.monitoring.network.monitor.ResourceUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SoftnetStatsMonitorTest
{
    private final RecordingSoftnetStatsHandler softnetStatsHandler = new RecordingSoftnetStatsHandler();
    private Path inputPath;
    private SoftnetStatsMonitor monitor;

    @Before
    public void before() throws Exception
    {
        inputPath = Files.createTempFile("proc-net-softnet_stat", "txt");
        ResourceUtil.writeDataFile("proc_net_softnet_stat_sample.txt", inputPath);
        monitor = new SoftnetStatsMonitor(inputPath);
    }

    @After
    public void after() throws Exception
    {
        Files.deleteIfExists(inputPath);
    }

    @Test
    public void shouldParseInput() throws Exception
    {
        monitor.poll(softnetStatsHandler);

        final List<SoftnetStatEntry> recordedEntries = softnetStatsHandler.getRecordedEntries();

        assertThat(recordedEntries.size(), is(4));
        assertEntry(recordedEntries.get(0), 0, 30864L, 0L, 0L);
        assertEntry(recordedEntries.get(1), 1, 66341L, 0L, 3599L);
        assertEntry(recordedEntries.get(2), 2, 60730L, 0L, 0L);
        assertEntry(recordedEntries.get(3), 3, 3182954591L, 17L, 1907884L);
    }


    @Test
    public void shouldNotNotifyOfUnchangedEntries() throws Exception
    {
        monitor.poll(softnetStatsHandler);

        softnetStatsHandler.getRecordedEntries().clear();

        ResourceUtil.writeDataFile("proc_net_softnet_stat_updated_sample.txt", inputPath);

        monitor.poll(softnetStatsHandler);

        final List<SoftnetStatEntry> recordedEntries = softnetStatsHandler.getRecordedEntries();

        assertThat(recordedEntries.size(), is(1));
        assertEntry(recordedEntries.get(0), 2, 60730L, 3L, 2L);
    }

    private static void assertEntry(final SoftnetStatEntry softnetStatEntry,
                                    final int cpuId,
                                    final long total,
                                    final long dropped,
                                    final long timeSqueeze)
    {
        assertThat("cpuid", softnetStatEntry.getCpuId(), is(cpuId));
        assertThat("processed", softnetStatEntry.getProcessed(), is(total));
        assertThat("dropped", softnetStatEntry.getDropped(), is(dropped));
        assertThat("squeezed", softnetStatEntry.getSqueezed(), is(timeSqueeze));
    }

    private static class RecordingSoftnetStatsHandler implements SoftnetStatsHandler
    {
        private final List<SoftnetStatEntry> recordedEntries = new ArrayList<>();

        @Override
        public void perCpuStatistics(final int cpuId, final long processed, final long squeezed, final long dropped)
        {
            recordedEntries.add(new SoftnetStatEntry(cpuId, processed, squeezed, dropped));
        }

        List<SoftnetStatEntry> getRecordedEntries()
        {
            return recordedEntries;
        }
    }
    
    private static final class SoftnetStatEntry
    {
        private final int cpuId;
        private final long processed;
        private final long squeezed;
        private final long dropped;

        private SoftnetStatEntry(final int cpuId, final long processed, final long squeezed, final long dropped)
        {
            this.cpuId = cpuId;
            this.processed = processed;
            this.squeezed = squeezed;
            this.dropped = dropped;
        }

        int getCpuId()
        {
            return cpuId;
        }

        long getProcessed()
        {
            return processed;
        }

        long getSqueezed()
        {
            return squeezed;
        }

        long getDropped()
        {
            return dropped;
        }
    }
}