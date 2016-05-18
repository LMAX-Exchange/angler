package com.epickrram.monitoring.network.monitor.util;

import org.junit.Test;

import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HexToLongDecoderTest
{
    @Test
    public void shouldDecodeSimpleNumber() throws Exception
    {
        assertValueDecoded("2D1E0A0A", encodeIpAddress(45, 30, 10, 10));
    }

    @Test
    public void shouldPortNumber() throws Exception
    {
        assertValueDecoded("4E50", 20048L);
    }

    private static long encodeIpAddress(final long a, final long b, final long c, final long d)
    {
        return a << 24 | b << 16 | c << 8 | d;
    }

    private static void assertValueDecoded(final String hexEncodedValue, final long expectedDecodedValue)
    {
        final ByteBuffer buffer = prepare(hexEncodedValue);
        final long value = HexToLongDecoder.decode(buffer, buffer.position(), buffer.limit());

        assertThat(value, is(expectedDecodedValue));
    }

    private static ByteBuffer prepare(final String hexEncodedValue)
    {
        return ByteBuffer.wrap(hexEncodedValue.getBytes(UTF_8));
    }
}