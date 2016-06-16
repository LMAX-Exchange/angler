package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class EncodedData2ObjectHashMapTest
{
    private static final String VALUE = "foobar";
    private static final EncodableKey KEY = new EncodableKey(17L);

    private final EncodedData2ObjectHashMap<EncodableKey, String> map =
            new EncodedData2ObjectHashMap<>(16, 1f, 8, this::encodeKey);


    @Test
    public void shouldContainKeyValuePair() throws Exception
    {
        map.put(KEY, VALUE);

        assertThat(map.containsKey(KEY), is(true));
        assertThat(map.get(KEY), is(VALUE));
    }

    @Test
    public void shouldUpdateExistingValue() throws Exception
    {
        final String updatedValue = "updatedValue";
        map.put(KEY, VALUE);
        map.put(KEY, updatedValue);

        assertThat(map.containsKey(KEY), is(true));
        assertThat(map.get(KEY), is(updatedValue));
    }

    @Test
    public void shouldRemoveValue() throws Exception
    {
        map.put(KEY, VALUE);
        final String removed = map.remove(KEY);

        assertThat(removed, is(VALUE));
        assertThat(map.containsKey(KEY), is(false));
        assertThat(map.get(KEY), is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfRemovedKeyIsNotPresent() throws Exception
    {
        final String removed = map.remove(KEY);

        assertThat(removed, is(nullValue()));
        assertThat(map.containsKey(KEY), is(false));
        assertThat(map.get(KEY), is(nullValue()));
    }

    @Ignore(value = "work in progress")
    @Test
    public void shouldHandleHashCollision() throws Exception
    {
        final EncodedData2ObjectHashMap<EncodableKey, String> map =
                new EncodedData2ObjectHashMap<>(16, 1f, 8, this::encodeKey, (k, buffer) -> 42);

        final EncodableKey otherKey = new EncodableKey(37L);
        final String otherValue = "other";

        map.put(KEY, VALUE);
        map.put(otherKey, otherValue);

        assertThat(map.containsKey(KEY), is(true));
        assertThat(map.get(KEY), is(VALUE));

        assertThat(map.containsKey(otherKey), is(true));
        assertThat(map.get(otherKey), is(otherValue));
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