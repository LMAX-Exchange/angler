package com.lmax.angler.monitoring.network.monitor.system.snmp;

import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsColumnHandler;
import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.util.FileHandler;
import com.lmax.angler.monitoring.network.monitor.util.FileLoader;
import com.lmax.angler.monitoring.network.monitor.util.Parsers;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Monitor for reporting changes in /proc/net/snmp.
 */
public final class SystemNetworkManagementMonitor
{
    private final FileHandler lineParser =
            Parsers.rowColumnHandler(new SnmpUdpStatisticsColumnHandler(this::onUpdate));
    private final FileLoader fileLoader;
    private SnmpUdpStatisticsHandler statisticsHandler;

    public SystemNetworkManagementMonitor()
    {
        this(Paths.get("/proc/net/snmp"));
    }

    SystemNetworkManagementMonitor(final Path pathToProcNetSnmp)
    {
        fileLoader = new FileLoader(pathToProcNetSnmp, 4096);
    }

    /**
     * Read from monitored file, report any changed values for UDP statistics.
     *
     * Not thread-safe, only call from a single thread.
     *
     * @param snmpUdpStatisticsHandler the handler for changed statistics
     */
    public void poll(final SnmpUdpStatisticsHandler snmpUdpStatisticsHandler)
    {
        this.statisticsHandler = snmpUdpStatisticsHandler;
        try
        {
            fileLoader.run(lineParser);
        }
        finally
        {
            this.statisticsHandler = null;
        }
    }

    private void onUpdate(final long inErrors, final long receiveBufferErrors, final long checksumErrors)
    {
        this.statisticsHandler.onStatisticsUpdated(inErrors, receiveBufferErrors, checksumErrors);
    }
}