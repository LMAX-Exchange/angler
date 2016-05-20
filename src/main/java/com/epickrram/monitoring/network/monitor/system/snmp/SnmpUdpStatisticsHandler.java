package com.epickrram.monitoring.network.monitor.system.snmp;

@FunctionalInterface
public interface SnmpUdpStatisticsHandler
{
    void onStatisticsUpdated(final long inErrors, final long receiveBufferErrors, final long inChecksumErrors);
}
