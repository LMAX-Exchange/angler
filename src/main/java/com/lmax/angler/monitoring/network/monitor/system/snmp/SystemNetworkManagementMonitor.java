package com.lmax.angler.monitoring.network.monitor.system.snmp;

import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.GlobalUdpStatistics;
import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsColumnHandler;
import com.lmax.angler.monitoring.network.monitor.system.snmp.udp.SnmpUdpStatisticsHandler;
import com.lmax.angler.monitoring.network.monitor.util.DelimitedDataParser;
import com.lmax.angler.monitoring.network.monitor.util.FileLoader;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class SystemNetworkManagementMonitor
{
    private final DelimitedDataParser columnParser = new DelimitedDataParser(new SnmpUdpStatisticsColumnHandler(this::onUpdate), (byte)' ', true);
    private final DelimitedDataParser lineParser = new DelimitedDataParser(columnParser, (byte)'\n', true);
    private final FileLoader fileLoader;
    private SnmpUdpStatisticsHandler statisticsHandler;

    public SystemNetworkManagementMonitor(final Path pathToProcNetSnmp)
    {
        fileLoader = new FileLoader(pathToProcNetSnmp, 4096);
    }

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

    private void onUpdate(final GlobalUdpStatistics globalUdpStatistics)
    {
        this.statisticsHandler.onStatisticsUpdated(
                globalUdpStatistics.getInErrors(),
                globalUdpStatistics.getReceiveBufferErrors(),
                globalUdpStatistics.getChecksumErrors());
    }
}