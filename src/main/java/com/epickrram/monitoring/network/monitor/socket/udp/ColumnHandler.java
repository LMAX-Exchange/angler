package com.epickrram.monitoring.network.monitor.socket.udp;

import com.epickrram.monitoring.network.monitor.util.BufferToString;
import com.epickrram.monitoring.network.monitor.util.TokenHandler;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

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
        final String tokenValue = BufferToString.bufferToString(src, startPosition, endPosition);
//        System.out.println("Current column: " + currentColumn + ", value: " + tokenValue);
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
                    System.out.println("local address: " + tokenValue);
                    break;
                case 4:
                    // do rx queue
                    System.out.println("rx queue: " + tokenValue);
                    break;
                case 9:
                    // do inode
                    break;
                case 12:
                    // do drops
                    System.out.println("drops: " + tokenValue);
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
        System.out.println("--complete--");
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
        System.out.println("--reset--");
        currentColumn = 0;
    }

    public long getSocketIdentifier()
    {
        return 0L;
    }

    public long getDrops()
    {
        return 0L;
    }

    public long getReceiveQueueDepth()
    {
        return 0L;
    }
}
