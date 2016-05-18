package com.epickrram.monitoring.network.monitor.system;

import com.epickrram.monitoring.network.monitor.util.DelimitedDataParser;
import org.agrona.collections.Int2ObjectHashMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SoftIrqHandlerTimeSqueezeMonitor
{
    private final Path pathToProcNetSoftnetStat;
    private final Int2ObjectHashMap<CpuSoftIrqData> cpuSoftIrqDataMap = new Int2ObjectHashMap<>();
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new SoftnetStatColumnHandler(this::handleEntry), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);

    private ByteBuffer buffer = ByteBuffer.allocateDirect(120 * Runtime.getRuntime().availableProcessors());
    private FileChannel fileChannel;
    private SoftnetStatsHandler softnetStatsHandler;
    private int cpuId;

    public SoftIrqHandlerTimeSqueezeMonitor(final Path pathToProcNetSoftnetStat)
    {
        this.pathToProcNetSoftnetStat = pathToProcNetSoftnetStat;
    }

    public void poll(final SoftnetStatsHandler softnetStatsHandler)
    {
        this.softnetStatsHandler = softnetStatsHandler;
        try
        {
            if (fileChannel == null)
            {
                fileChannel = FileChannel.open(pathToProcNetSoftnetStat, StandardOpenOption.READ);
            }
            final long fileSize = Files.size(pathToProcNetSoftnetStat);

            if (fileSize > buffer.capacity())
            {
                buffer = ByteBuffer.allocateDirect((int) Math.max(buffer.capacity() * 2, fileSize));
            }

            buffer.clear();
            fileChannel.read(buffer, 0);
            buffer.flip();

            cpuId = 0;
            lineParser.reset();
            lineParser.handleToken(buffer, buffer.position(), buffer.limit());
        }
        catch(final IOException e)
        {
            throw new UncheckedIOException(e);
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