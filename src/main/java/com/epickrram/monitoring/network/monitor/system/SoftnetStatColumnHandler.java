package com.epickrram.monitoring.network.monitor.system;

import com.epickrram.monitoring.network.monitor.util.HexToLongDecoder;
import com.epickrram.monitoring.network.monitor.util.TokenHandler;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public final class SoftnetStatColumnHandler implements TokenHandler
{
    private final Consumer<CpuSoftIrqData> cpuSoftIrqDataConsumer;
    private final CpuSoftIrqData entry = new CpuSoftIrqData(-1);
    private int currentColumn = 0;

    public SoftnetStatColumnHandler(final Consumer<CpuSoftIrqData> cpuSoftIrqDataConsumer)
    {
        this.cpuSoftIrqDataConsumer = cpuSoftIrqDataConsumer;
    }

    @Override
    public void handleToken(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        switch (currentColumn)
        {
            case 0:
                // total
                //001d1cac
                final long total = HexToLongDecoder.LOWER_CASE.decode(src, startPosition, endPosition);
                entry.setTotalPackets(total);
                break;
            case 1:
                // dropped
                //001d1cac
                final long dropped = HexToLongDecoder.LOWER_CASE.decode(src, startPosition, endPosition);
                entry.setDroppedPackets(dropped);
                break;
            case 2:
                // squeeze
                //001d1cac
                final long squeeze = HexToLongDecoder.LOWER_CASE.decode(src, startPosition, endPosition);
                entry.setTimeSqueeze(squeeze);
                break;
            default:
                break;
        }
        currentColumn++;
    }

    @Override
    public void complete()
    {
        cpuSoftIrqDataConsumer.accept(entry);
        currentColumn = 0;
        entry.reset();
    }

    @Override
    public void reset()
    {
        currentColumn = 0;
    }
}
