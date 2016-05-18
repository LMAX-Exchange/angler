package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.util.AsciiBytesToLongDecoder;
import com.epickrram.monitoring.network.monitor.util.HexToLongDecoder;
import com.epickrram.monitoring.network.monitor.util.TokenHandler;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static com.epickrram.monitoring.network.monitor.socket.SocketIdentifier.fromLinuxKernelHexEncodedAddressAndPort;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class ColumnHandler implements TokenHandler
{
    private static final short HEADER_ROW_FIRST_COLUMN_VALUE = ByteBuffer.wrap("sl".getBytes(UTF_8)).getShort();
    private final Consumer<BufferStatsEntry> bufferStatsEntryConsumer;
    private final BufferStatsEntry entry = new BufferStatsEntry();
    private int currentColumn = 0;
    private boolean headerRow;

    public ColumnHandler(final Consumer<BufferStatsEntry> bufferStatsEntryConsumer)
    {
        this.bufferStatsEntryConsumer = bufferStatsEntryConsumer;
    }

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
            switch (currentColumn)
            {
                case 1:
                    // do local address
                    //00000000:4E50
                    final long socketIpv4Address = HexToLongDecoder.decode(src, startPosition, startPosition + 8);
                    final long socketPortNumber = HexToLongDecoder.decode(src, startPosition + 9, endPosition);
                    entry.setSocketIdentifier(fromLinuxKernelHexEncodedAddressAndPort(socketIpv4Address, socketPortNumber));
                    break;
                case 4:
                    // do rx queue
                    // hex
                    //00000000:00000000
                    final long receiveQueueDepth = HexToLongDecoder.decode(src, startPosition + 9, endPosition);
                    entry.setReceiveQueueDepth(receiveQueueDepth);
                    break;
                case 9:
                    // do inode
                    final long inode = AsciiBytesToLongDecoder.decode(src, startPosition, endPosition);
                    entry.setInode(inode);
                    break;
                case 12:
                    // do drops
                    final long drops = AsciiBytesToLongDecoder.decode(src, startPosition, endPosition);
                    entry.setDrops(drops);
                    //0
                    break;
                default:
                    break;
            }
        }

        currentColumn++;
    }

    @Override
    public void complete()
    {
        if(!headerRow)
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
