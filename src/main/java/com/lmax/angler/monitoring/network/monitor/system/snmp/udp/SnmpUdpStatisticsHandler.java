package com.lmax.angler.monitoring.network.monitor.system.snmp.udp;

/**
 * Callback for UDP-specific statistics in /proc/net/snmp.
 */
@FunctionalInterface
public interface SnmpUdpStatisticsHandler
{
    /**
     * Callback method.
     * @param inErrors InErrors count
     * @param receiveBufferErrors RecvbufErrors count
     * @param inChecksumErrors InCsumErrors count
     */
    void onStatisticsUpdated(final long inErrors, final long receiveBufferErrors, final long inChecksumErrors);
}
