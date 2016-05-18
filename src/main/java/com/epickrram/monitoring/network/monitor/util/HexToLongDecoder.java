package com.epickrram.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public final class HexToLongDecoder
{
    private HexToLongDecoder() {}

    public static long decode(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        final int length = endPosition - startPosition;
        long decodedValue = 0L;
        decodedValue |= hexValueAtOffset(src, startPosition) << 24;
        decodedValue |= hexValueAtOffset(src, startPosition + 2) << 16;
        decodedValue |= hexValueAtOffset(src, startPosition + 4) << 8;
        decodedValue |= hexValueAtOffset(src, startPosition + 6);
        return decodedValue;
    }

    private static int hexValueAtOffset(final ByteBuffer src, final int offset)
    {
        int value = getDecimalValueOfHexDigit(src, offset);

        value <<= 4;

        return value | getDecimalValueOfHexDigit(src, offset + 1);
    }

    private static int getDecimalValueOfHexDigit(final ByteBuffer src, final int offset)
    {
        final byte first = src.get(offset);
        int value;
        if(first >= '0' && first <= '9')
        {
            value = first - '0';
        }
        else if(first >= 'A' && first <= 'F')
        {
            value = first - 'A' + 10;
        }
        else
        {
            throw new IllegalArgumentException("Unable to process char: " + first);
        }
        return value;
    }
}
