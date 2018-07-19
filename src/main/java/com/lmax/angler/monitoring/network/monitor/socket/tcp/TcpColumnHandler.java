package com.lmax.angler.monitoring.network.monitor.socket.tcp;

import com.lmax.angler.monitoring.network.monitor.socket.SocketIdentifier;
import com.lmax.angler.monitoring.network.monitor.util.AsciiBytesToLongDecoder;
import com.lmax.angler.monitoring.network.monitor.util.HexToLongDecoder;
import com.lmax.angler.monitoring.network.monitor.util.TokenHandler;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TokenHandler for entries in /proc/net/udp.
 */
final class TcpColumnHandler implements TokenHandler
{
    private static final int TCP_SOCKET_IDENTIFIER_COLUMN =
            Integer.getInteger("angler.tcp.socketIdentifierColumn", 1);
    private static final short HEADER_ROW_FIRST_COLUMN_VALUE = ByteBuffer.wrap("sl".getBytes(UTF_8)).getShort();
    private final Consumer<TcpStatsEntry> bufferStatsEntryConsumer;
    private final TcpStatsEntry entry = new TcpStatsEntry();
    private int currentColumn = 0;
    private boolean headerRow;

    public TcpColumnHandler(final Consumer<TcpStatsEntry> bufferStatsEntryConsumer)
    {
        this.bufferStatsEntryConsumer = bufferStatsEntryConsumer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleToken(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        if(src.getShort(startPosition) == HEADER_ROW_FIRST_COLUMN_VALUE)
        {
            // header row
            headerRow = true;
        }

        if(!headerRow)
        {
            if (currentColumn == TCP_SOCKET_IDENTIFIER_COLUMN)
            {
                // do local address
                //00000000:4E50
                final long socketIpv4Address = HexToLongDecoder.UPPER_CASE.decodeHex(src, startPosition, startPosition + 8);
                final long socketPortNumber = HexToLongDecoder.UPPER_CASE.decodeHex(src, startPosition + 9, endPosition);
                entry.setSocketIdentifier(SocketIdentifier.fromLinuxKernelHexEncodedAddressAndPort(socketIpv4Address, socketPortNumber));

            }
            else if (currentColumn == 4)
            {
                // do tx/rx queue
                // hex
                //00000000:00000000
                final long transmitQueueDepth = HexToLongDecoder.UPPER_CASE.decodeHex(src, startPosition, startPosition + 8);
                final long receiveQueueDepth = HexToLongDecoder.UPPER_CASE.decodeHex(src, startPosition + 9, endPosition);
                entry.setTransmitQueueDepth(transmitQueueDepth);
                entry.setReceiveQueueDepth(receiveQueueDepth);

            }
            else if (currentColumn == 9)
            {
                // do inode
                final long inode = AsciiBytesToLongDecoder.decodeAscii(src, startPosition, endPosition);
                entry.setInode(inode);

            }
        }

        currentColumn++;
    }

    @Override
    public void complete()
    {
        if(!headerRow && currentColumn != 0)
        {
            bufferStatsEntryConsumer.accept(entry);
        }
        headerRow = false;
        currentColumn = 0;
        entry.reset();
    }

    @Override
    public void reset()
    {
        currentColumn = 0;
    }
}
