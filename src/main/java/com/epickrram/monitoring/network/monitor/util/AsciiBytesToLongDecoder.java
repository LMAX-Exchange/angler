package com.epickrram.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public final class AsciiBytesToLongDecoder
{
    private AsciiBytesToLongDecoder() {}

    public static long decode(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        final int length = endPosition - startPosition;
        if(length == 0)
        {
            throw new IllegalArgumentException("Cannot decode zero-length ascii string.");
        }
        long decoded = 0L;
        for(int offset = 0; offset < length; offset++)
        {
            decoded *= 10;

            final byte digit = src.get(startPosition + offset);

            if(digit < '0' || digit > '9')
            {
                throw new IllegalArgumentException("Invalid digit: " + (char) digit);
            }

            decoded += digit - '0';
        }

        return decoded;
    }
}
