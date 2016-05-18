package com.epickrram.monitoring.network.monitor.system;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Long.parseLong;
import static java.nio.file.Files.readAllLines;

public final class SoftIrqHandlerTimeSqueezeMonitor
{
    private static final Path SOFTNET_STAT_FILE = Paths.get("/proc/net/softnet_stat");
    private final Map<Integer, CpuSoftIrqData> cpuSoftIrqDataMap = new HashMap<>();

    public void report()
    {
        try
        {
            final AtomicInteger cpuId = new AtomicInteger(0);
            readAllLines(SOFTNET_STAT_FILE).stream().
                    forEach(l -> parse(l, cpuId.getAndIncrement()));

            cpuSoftIrqDataMap.values().stream().
                    filter(d -> d.droppedPackets != 0 || d.timeSqueeze != 0).
                    filter(d -> d.changed).
                    forEach(d -> {
                        System.out.printf("CPU: %d, received: %d, dropped: %d, squeezed: %d%n",
                                d.cpuId, d.totalPackets, d.droppedPackets, d.timeSqueeze);
                    });
        }
        catch (final IOException e)
        {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    private void parse(final String line, final int lineIndex)
    {
        final String[] tokens = line.split("\\s+");
        final long totalPackets = parseLong(tokens[0], 16);
        final long droppedPackets = parseLong(tokens[1], 16);
        final long timeSqueeze = parseLong(tokens[2], 16);
        final CpuSoftIrqData cpuSoftIrqData = cpuSoftIrqDataMap.computeIfAbsent(lineIndex, k -> new CpuSoftIrqData(
                lineIndex,
                totalPackets,
                droppedPackets,
                timeSqueeze));

        cpuSoftIrqData.update(totalPackets, droppedPackets, timeSqueeze);
    }

    private static final class CpuSoftIrqData
    {
        private final int cpuId;
        private long totalPackets;
        private long droppedPackets;
        private long timeSqueeze;
        private boolean changed;

        public CpuSoftIrqData(
                final int cpuId,
                final long totalPackets,
                final long droppedPackets,
                final long timeSqueeze)
        {
            this.cpuId = cpuId;
            this.totalPackets = totalPackets;
            this.droppedPackets = droppedPackets;
            this.timeSqueeze = timeSqueeze;
        }

        public void update(final long totalPackets,
                           final long droppedPackets,
                           final long timeSqueeze)
        {
            changed = false;
            if(totalPackets != this.totalPackets)
            {
                this.totalPackets = totalPackets;
            }

            if(droppedPackets != this.droppedPackets)
            {
                this.droppedPackets = droppedPackets;
                changed = true;
            }

            if(timeSqueeze != this.timeSqueeze)
            {
                this.timeSqueeze = timeSqueeze;
                changed = true;
            }
        }
    }
}