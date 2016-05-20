package com.lmax.angler.monitoring.network.monitor.system.softnet;

/**
 * Callback for statistics from /proc/net/softnet_stat.
 */
@FunctionalInterface
public interface SoftnetStatsHandler
{
    /**
     * Callback method.
     * @param cpuId zero-based CPU that these statistics refer to
     * @param processed softIRQ events processed
     * @param squeezed squeeze events
     * @param dropped dropped events
     */
    void perCpuStatistics(final int cpuId, final long processed, final long squeezed, final long dropped);
}
