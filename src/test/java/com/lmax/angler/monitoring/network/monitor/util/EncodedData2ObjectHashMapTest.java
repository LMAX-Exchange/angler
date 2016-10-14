package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class EncodedData2ObjectHashMapTest
{
    private static final String VALUE = "foobar";
    private static final EncodableKey KEY = new EncodableKey(37L);
    private static final EncodableKey NULL_KEY = new EncodableKey(Long.MIN_VALUE);
    private static final int INITIAL_CAPACITY = 16;

    private final EncodedData2ObjectHashMap<EncodableKey, String> map =
            new EncodedData2ObjectHashMap<>(INITIAL_CAPACITY, 1f, 8, this::encodeKey, NULL_KEY);

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

    @Test
    public void shouldHandleHashCollision() throws Exception
    {
        final EncodedData2ObjectHashMap<EncodableKey, String> map =
                new EncodedData2ObjectHashMap<>(INITIAL_CAPACITY, 1f, 8, this::encodeKey, (buffer) -> 42, NULL_KEY);

        final EncodableKey otherKey = new EncodableKey(147L);
        final String otherValue = "other";

        map.put(KEY, VALUE);
        map.put(otherKey, otherValue);

        assertThat(map.containsKey(KEY), is(true));
        assertThat(map.get(KEY), is(VALUE));

        assertThat(map.containsKey(otherKey), is(true));
        assertThat(map.get(otherKey), is(otherValue));
    }

    @Test
    public void shouldTrackSize() throws Exception
    {
        assertThat(map.size(), is(0));

        map.put(KEY, VALUE);

        assertThat(map.size(), is(1));

        map.put(new EncodableKey(23L), VALUE);

        assertThat(map.size(), is(2));

        map.remove(KEY);

        assertThat(map.size(), is(1));
    }

    @Test
    public void shouldNotBeEmptyAfterInsertion() throws Exception
    {
        map.put(KEY, VALUE);

        assertThat(map.isEmpty(), is(false));
    }

    @Test
    public void shouldBeEmptyAfterClear() throws Exception
    {
        map.put(KEY, VALUE);

        map.clear();

        assertThat(map.isEmpty(), is(true));
        assertThat(map.containsKey(KEY), is(false));
    }

    @Test
    public void shouldContainValue() throws Exception
    {
        map.put(KEY, VALUE);

        assertThat(map.containsValue(VALUE), is(true));
        assertThat(map.containsValue(new Object()), is(false));
    }

    @Test
    public void shouldIncreaseInSizeWhenNecessary() throws Exception
    {
        for(int i = 0; i < INITIAL_CAPACITY; i++)
        {
            map.put(new EncodableKey(i), VALUE);
        }

        final EncodableKey key = new EncodableKey(INITIAL_CAPACITY);
        final String otherValue = "otherValue";
        final String previousValue = map.put(key, otherValue);

        assertThat(previousValue, is(nullValue()));

        assertThat(map.get(key), is(otherValue));
    }

    @Test
    public void previousValuesShouldStillExistAfterResize() throws Exception
    {
        for(int i = 0; i < INITIAL_CAPACITY; i++)
        {
            map.put(new EncodableKey(i), VALUE + "_" + i);
            assertThat(map.get(new EncodableKey(i)), is(VALUE + "_" + i));
        }

        map.put(new EncodableKey(INITIAL_CAPACITY), "otherValue");

        for(int i = 0; i < INITIAL_CAPACITY; i++)
        {
            assertThat(map.get(new EncodableKey(i)), is(VALUE + "_" + i));
        }
    }

    @Test
    public void shouldCompactEmptyEntriesWhenCollidingKeyIsRemoved() throws Exception
    {
        final EncodedData2ObjectHashMap<EncodableKey, String> map =
                new EncodedData2ObjectHashMap<>(INITIAL_CAPACITY, 1f, 8, this::encodeKey, (buffer) -> 5, NULL_KEY);

        final EncodableKey keyOne = new EncodableKey(147L);
        final EncodableKey keyTwo = new EncodableKey(37L);

        map.put(keyOne, VALUE);
        map.put(keyTwo, VALUE);

        final int peekKeyOnePosition = map.getIndexOfKey(keyOne);
        final int peekKeyTwoPosition = map.getIndexOfKey(keyTwo);

        assertThat(peekKeyOnePosition, is(peekKeyTwoPosition - 1));

        map.remove(keyOne);

        assertThat(map.getIndexOfKey(keyTwo), is(peekKeyOnePosition));
    }

    @Test
    public void shouldCompactEmptyEntriesWhenDifferentKeyHashHasBeenOccupiedByPreviousEntry() throws Exception
    {
        // map that will hash even keys to index 5, odd keys to index 6
        final EncodedData2ObjectHashMap<EncodableKey, String> map =
                new EncodedData2ObjectHashMap<>(INITIAL_CAPACITY, 1f, 8, this::encodeKey, (buffer) -> {
                    if(buffer.get(7) % 2 == 0)
                    {
                        return 5;
                    }
                    else
                    {
                        return 6;
                    }
                }, NULL_KEY);

        final EncodableKey keyA = new EncodableKey(4L);
        final EncodableKey keyB = new EncodableKey(8L);
        final EncodableKey keyC = new EncodableKey(9L);
        final String evenValueOne = VALUE + 4;
        final String evenValueTwo = VALUE + 8;
        final String oddValue = VALUE + 9;

        // initial map state:
        // key A -> hashes to index 5, inserted at 5
        // key B -> hashes to index 5, inserted at 6
        // key C -> hashes to index 6, inserted at 7

        map.put(keyA, evenValueOne);
        map.put(keyB, evenValueTwo);
        map.put(keyC, oddValue);

        assertThat(map.getIndexOfKey(keyA), is(5));
        assertThat(map.get(keyA), is(evenValueOne));
        assertThat(map.getIndexOfKey(keyB), is(6));
        assertThat(map.get(keyB), is(evenValueTwo));
        assertThat(map.getIndexOfKey(keyC), is(7));
        assertThat(map.get(keyC), is(oddValue));

        // remove key A
        assertThat(map.remove(keyA), is(evenValueOne));

        // expected map state:
        // key B -> moved to index 5
        // key C -> moved to index 6
        assertThat(map.getIndexOfKey(keyB), is(5));
        assertThat(map.get(keyB), is(evenValueTwo));
        assertThat(map.getIndexOfKey(keyC), is(6));
        assertThat(map.get(keyC), is(oddValue));
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