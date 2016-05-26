package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncodedData2ObjectHashMapTest
{
    private static final String VALUE = "foobar";
    private final EncodedData2ObjectHashMap<EncodableKey, String> map =
            new EncodedData2ObjectHashMap<>(16, 1f, 8, this::encodeKey);


    @Test
    public void shouldContainKeyValuePair() throws Exception
    {
        final EncodableKey key = new EncodableKey(17L);
        map.put(key, VALUE);

        assertThat(map.containsKey(key), is(true));
        assertThat(map.get(key), is(VALUE));
    }

    private static final class EncodableKey
    {
        private long value;

        private EncodableKey(final long value)
        {
            this.value = value;
        }
    }

    private void encodeKey(final EncodableKey key, final ByteBuffer keyBuffer)
    {
        keyBuffer.putLong(key.value);
    }
}