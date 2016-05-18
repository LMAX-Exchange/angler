package com.epickrram.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public final class HexToLongDecoder
{
    private HexToLongDecoder() {}

    public static long decode(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        final int length = endPosition - startPosition;
        if(length > 8)
        {
            throw new IllegalArgumentException("Cannot decode hex long value. Specified value is too long.");
        }
        if(length == 0)
        {
            throw new IllegalArgumentException("Cannot decode zero-length hex value.");
        }
        if(Integer.bitCount(length) != 1)
        {
            throw new IllegalArgumentException("Can only decode hex values that are a multiple of 2 in length.");
        }

        long decodedValue = 0L;
        int shift = (length - 2) << 2;
        for(int offset = 0; offset < length; offset += 2)
        {
            decodedValue |= hexValueAtOffset(src, startPosition + offset) << shift;
            shift -= 8;
        }
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
