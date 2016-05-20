package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

/**
 * Converts ASCII integer characters to a long value.
 */
public final class AsciiBytesToLongDecoder
{
    private AsciiBytesToLongDecoder() {}

    /**
     * Decode an integer from a sequence of ASCII bytes.
     * @param src ASCII bytes
     * @param startPosition start position in the source buffer
     * @param endPosition end position in the source buffer
     * @return the decoded value
     */
    public static long decodeAscii(final ByteBuffer src, final int startPosition, final int endPosition)
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
