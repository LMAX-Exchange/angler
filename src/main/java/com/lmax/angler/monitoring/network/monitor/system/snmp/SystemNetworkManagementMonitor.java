package com.lmax.angler.monitoring.network.monitor.system.snmp;

import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsColumnHandler;
import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.util.DelimitedDataParser;
import com.lmax.angler.monitoring.network.monitor.util.FileLoader;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Monitor for reporting changes in /proc/net/snmp.
 */
public final class SystemNetworkManagementMonitor
{
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new SnmpUdpStatisticsColumnHandler(this::onUpdate), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);
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
     * @param snmpUdpStatisticsHandler the handler for changed statistics
     */
    public void poll(final SnmpUdpStatisticsHandler snmpUdpStatisticsHandler)
    {
        this.statisticsHandler = snmpUdpStatisticsHandler;
        try
        {
            fileLoader.load();
            final ByteBuffer buffer = fileLoader.getBuffer();

            lineParser.reset();
            lineParser.handleToken(buffer, buffer.position(), buffer.limit());
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