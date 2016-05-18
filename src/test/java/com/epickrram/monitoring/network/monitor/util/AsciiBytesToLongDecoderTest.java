package com.epickrram.monitoring.network.monitor.util;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsciiBytesToLongDecoderTest
{
    @Test
    public void shouldDecodeSimpleNumber() throws Exception
    {
        assertAsciiDecode("1234", 1234L);
    }

    private void assertAsciiDecode(final String asciiNumber, final long expected)
    {
        final ByteBuffer src = ByteBuffer.wrap(asciiNumber.getBytes(StandardCharsets.UTF_8));
        final long actual = AsciiBytesToLongDecoder.decode(src, src.position(), src.limit());
        assertThat(actual, is(expected));
    }
}