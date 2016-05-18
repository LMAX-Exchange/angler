package com.epickrram.monitoring.network.monitor.system;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.copy;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SoftIrqHandlerTimeSqueezeMonitorTest
{
    private final RecordingSoftnetStatsHandler softnetStatsHandler = new RecordingSoftnetStatsHandler();
    private Path inputPath;
    private SoftIrqHandlerTimeSqueezeMonitor monitor;

    @Before
    public void before() throws Exception
    {
        inputPath = Files.createTempFile("proc-net-udp", "txt");
        writeDataFile("proc_net_softnet_stat_sample.txt");
        monitor = new SoftIrqHandlerTimeSqueezeMonitor(inputPath);
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

        writeDataFile("proc_net_softnet_stat_updated_sample.txt");

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
        assertThat(softnetStatEntry.getCpuId(), is(cpuId));
        assertThat(softnetStatEntry.getProcessed(), is(total));
        assertThat(softnetStatEntry.getDropped(), is(dropped));
        assertThat(softnetStatEntry.getSqueezed(), is(timeSqueeze));
    }

    private void writeDataFile(final String resourceName) throws IOException, URISyntaxException
    {
        copy(Paths.get(currentThread().getContextClassLoader().getResource(resourceName).toURI()),
                new FileOutputStream(inputPath.toFile(), false));
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