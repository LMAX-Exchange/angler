package com.lmax.angler.monitoring.network.monitor.system.softnet;

@FunctionalInterface
public interface SoftnetStatsHandler
{
    void perCpuStatistics(final int cpuId, final long processed, final long squeezed, final long dropped);
}
