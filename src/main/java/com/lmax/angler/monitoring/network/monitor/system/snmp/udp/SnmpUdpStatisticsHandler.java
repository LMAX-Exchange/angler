package com.lmax.angler.monitoring.network.monitor.system.snmp.udp;

@FunctionalInterface
public interface SnmpUdpStatisticsHandler
{
    void onStatisticsUpdated(final long inErrors, final long receiveBufferErrors, final long inChecksumErrors);
}
