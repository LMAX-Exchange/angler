package com.lmax.angler.monitoring.network.monitor.system.softnet;

import com.lmax.angler.monitoring.network.monitor.util.DelimitedDataParser;
import com.lmax.angler.monitoring.network.monitor.util.FileLoader;
import org.agrona.collections.Int2ObjectHashMap;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Runtime.getRuntime;

/**
 * Monitor for reporting changes in /proc/net/softnet_stat, which can indicate that the kernel thread
 * responsible for processing incoming network softIRQs is unable to keep up with the ingress rate.
 */
public final class SoftnetStatsMonitor
{
    private static final int ESTIMATED_LINE_LENGTH = 120;

    private final Int2ObjectHashMap<CpuSoftIrqData> cpuSoftIrqDataMap = new Int2ObjectHashMap<>();
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new SoftnetStatColumnHandler(this::handleEntry), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);
    private final FileLoader fileLoader;

    private SoftnetStatsHandler softnetStatsHandler;
    private int cpuId;

    public SoftnetStatsMonitor()
    {
        this(Paths.get("/proc/net/softnet_stat"));
    }

    SoftnetStatsMonitor(final Path pathToProcNetSoftnetStat)
    {
        fileLoader = new FileLoader(pathToProcNetSoftnetStat, ESTIMATED_LINE_LENGTH * getRuntime().availableProcessors());
    }

    /**
     * Read from monitored file, report any changed values to the supplied handler.
     *
     * Not thread-safe, only call from a single thread.
     *
     * @param softnetStatsHandler the handler for changed statistics
     */
    public void poll(final SoftnetStatsHandler softnetStatsHandler)
    {
        this.softnetStatsHandler = softnetStatsHandler;
        try
        {
            fileLoader.load();
            final ByteBuffer buffer = fileLoader.getBuffer();

            cpuId = 0;
            lineParser.reset();
            lineParser.handleToken(buffer, buffer.position(), buffer.limit());
        }
        finally
        {
            this.softnetStatsHandler = null;
        }
    }

    private void handleEntry(final CpuSoftIrqData cpuSoftIrqData)
    {
        final int currentCpuId = this.cpuId++;
        if(!cpuSoftIrqDataMap.containsKey(currentCpuId))
        {
            cpuSoftIrqDataMap.put(currentCpuId, new CpuSoftIrqData(currentCpuId));
        }

        final CpuSoftIrqData existing = cpuSoftIrqDataMap.get(currentCpuId);
        existing.copyFrom(cpuSoftIrqData);
        if(existing.isChanged())
        {
            softnetStatsHandler.perCpuStatistics(
                    existing.getCpuId(), existing.getTotalPackets(),
                    existing.getTimeSqueeze(), existing.getDroppedPackets());
        }

    }
}