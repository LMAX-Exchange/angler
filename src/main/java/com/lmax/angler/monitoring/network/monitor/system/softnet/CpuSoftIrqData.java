package com.lmax.angler.monitoring.network.monitor.system.softnet;

/**
 * Value object
 */
final class CpuSoftIrqData
{
    private final int cpuId;
    private long totalPackets;
    private long droppedPackets;
    private long timeSqueeze;
    private boolean changed;

    CpuSoftIrqData(final int cpuId)
    {
        this.cpuId = cpuId;
    }

    void copyFrom(final CpuSoftIrqData source)
    {
        update(source.getTotalPackets(), source.getDroppedPackets(), source.getTimeSqueeze());
    }

    int getCpuId()
    {
        return cpuId;
    }

    long getTotalPackets()
    {
        return totalPackets;
    }

    long getDroppedPackets()
    {
        return droppedPackets;
    }

    long getTimeSqueeze()
    {
        return timeSqueeze;
    }

    void setTotalPackets(final long totalPackets)
    {
        this.totalPackets = totalPackets;
    }

    void setDroppedPackets(final long droppedPackets)
    {
        this.droppedPackets = droppedPackets;
    }

    void setTimeSqueeze(final long timeSqueeze)
    {
        this.timeSqueeze = timeSqueeze;
    }

    boolean isChanged()
    {
        return changed;
    }

    void reset()
    {
        totalPackets = 0;
        droppedPackets = 0;
        timeSqueeze = 0;
    }

    private void update(final long totalPackets,
                        final long droppedPackets,
                        final long timeSqueeze)
    {
        changed = false;
        if(totalPackets != this.totalPackets)
        {
            this.totalPackets = totalPackets;
            changed = true;
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
