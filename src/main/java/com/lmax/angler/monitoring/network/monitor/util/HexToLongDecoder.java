package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public enum HexToLongDecoder
{
    LOWER_CASE(false),
    UPPER_CASE(true);

    private static final byte ASCII_ZERO = (byte) '0';
    private static final byte ASCII_NINE = (byte) '9';
    private static final byte ASCII_A_UPPERCASE = (byte) 'A';
    private static final byte ASCII_A_LOWERCASE = (byte) 'a';
    private static final byte ASCII_F_UPPERCASE = (byte) 'F';
    private static final byte ASCII_F_LOWERCASE = (byte) 'f';

    private final boolean isUpperCase;

    HexToLongDecoder(final boolean isUpperCase)
    {
        this.isUpperCase = isUpperCase;
    }

    public long decodeHex(final ByteBuffer src, final int startPosition, final int endPosition)
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
            decodedValue |= Integer.toUnsignedLong(hexValueAtOffset(src, startPosition + offset) << shift);
            shift -= 8;
        }
        return decodedValue;
    }

    private int hexValueAtOffset(final ByteBuffer src, final int offset)
    {
        int value = getDecimalValueOfHexDigit(src, offset);

        value <<= 4;

        return value | getDecimalValueOfHexDigit(src, offset + 1);
    }

    private int getDecimalValueOfHexDigit(final ByteBuffer src, final int offset)
    {
        final byte first = src.get(offset);
        int value;
        if(first >= ASCII_ZERO && first <= ASCII_NINE)
        {
            value = (first - ASCII_ZERO);
        }
        else if(isUpperCase && (first >= ASCII_A_UPPERCASE && first <= ASCII_F_UPPERCASE))
        {
            value = (first - ASCII_A_UPPERCASE + 10);
        }
        else if(!isUpperCase && (first >= ASCII_A_LOWERCASE && first <= ASCII_F_LOWERCASE))
        {
            value = (first - ASCII_A_LOWERCASE + 10);
        }
        else
        {
            throw new IllegalArgumentException("Unable to process char: " + first);
        }
        return value;
    }
}
