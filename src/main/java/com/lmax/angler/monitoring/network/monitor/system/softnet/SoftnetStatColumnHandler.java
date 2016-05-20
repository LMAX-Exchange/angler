package com.lmax.angler.monitoring.network.monitor.system.softnet;

import com.lmax.angler.monitoring.network.monitor.util.TokenHandler;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static com.lmax.angler.monitoring.network.monitor.util.HexToLongDecoder.LOWER_CASE;

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
                final long total = LOWER_CASE.decodeHex(src, startPosition, endPosition);
                entry.setTotalPackets(total);
                break;
            case 1:
                // dropped
                //001d1cac
                final long dropped = LOWER_CASE.decodeHex(src, startPosition, endPosition);
                entry.setDroppedPackets(dropped);
                break;
            case 2:
                // squeeze
                //001d1cac
                final long squeeze = LOWER_CASE.decodeHex(src, startPosition, endPosition);
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
