package com.lmax.angler.monitoring.network.monitor.system.snmp.udp;

import com.lmax.angler.monitoring.network.monitor.util.TokenHandler;

import java.nio.ByteBuffer;

import static com.lmax.angler.monitoring.network.monitor.util.AsciiBytesToLongDecoder.decodeAscii;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TokenHandler for UDP-specific data in /proc/net/snmp.
 */
public final class SnmpUdpStatisticsColumnHandler implements TokenHandler
{
    private static final long HEADER_ROW_FIRST_COLUMN_VALUE = ByteBuffer.wrap("Udp: InD".getBytes(UTF_8)).getLong();
    private static final int UDP_ROW_FIRST_COLUMN_VALUE = ByteBuffer.wrap("Udp:".getBytes(UTF_8)).getInt();
    private final SnmpUdpStatisticsHandler globalUdpStatisticsConsumer;
    private final GlobalUdpStatistics entry = new GlobalUdpStatistics();
    private int currentColumn = 0;
    private boolean currentRowIsUdpDataRow;

    public SnmpUdpStatisticsColumnHandler(final SnmpUdpStatisticsHandler globalUdpStatisticsConsumer)
    {
        this.globalUdpStatisticsConsumer = globalUdpStatisticsConsumer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleToken(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        switch (currentColumn)
        {
            case 0:
                if(src.getInt(startPosition) == UDP_ROW_FIRST_COLUMN_VALUE &&
                   src.getLong(startPosition) != HEADER_ROW_FIRST_COLUMN_VALUE)
                {
                    currentRowIsUdpDataRow = true;
                }
                break;
            case 3:
                // InErrors
                if(currentRowIsUdpDataRow)
                {
                    entry.setInErrors(decodeAscii(src, startPosition, endPosition));
                }
                break;
            case 5:
                // RcvBufErrors
                if(currentRowIsUdpDataRow)
                {
                    entry.setReceiveBufferErrors(decodeAscii(src, startPosition, endPosition));
                }
                break;
            case 7:
                // InChksumErrors
                if(currentRowIsUdpDataRow)
                {
                    entry.setChecksumErrors(decodeAscii(src, startPosition, endPosition));
                }
                break;
            default:
                break;
        }
        currentColumn++;
    }

    @Override
    public void complete()
    {
        if(currentRowIsUdpDataRow)
        {
            globalUdpStatisticsConsumer.onStatisticsUpdated(entry.getInErrors(), entry.getReceiveBufferErrors(), entry.getChecksumErrors());
            currentRowIsUdpDataRow = false;
        }
        currentColumn = 0;
        entry.reset();
    }

}
