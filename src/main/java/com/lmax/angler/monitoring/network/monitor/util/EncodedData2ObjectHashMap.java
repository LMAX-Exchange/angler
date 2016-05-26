package com.lmax.angler.monitoring.network.monitor.util;

import org.agrona.BitUtil;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Primarily copied from Agron Long2ObjectHashMap, with scope for arbitrarily long keys.
 *
 * Keys must be fixed-length, and capable of being encoded into a ByteBuffer.
 *
 * @param <V> the value type
 */
public final class EncodedData2ObjectHashMap<K, V> implements Map<K, V>
{
    private final int keyLengthInBytes;
    private final ByteBuffer keyBuffer;
    private final float loadFactor;
    private final BiConsumer<K, ByteBuffer> keyEncoder;

    private ByteBuffer keySpace;
    private Object[] values;
    private int resizeThreshold;
    private int capacity;

    public EncodedData2ObjectHashMap(
            final int initialCapacity,
            final float loadFactor,
            final int keyLengthInBytes,
            final BiConsumer<K, ByteBuffer> keyEncoder)
    {
        this.capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        this.loadFactor = loadFactor;
        this.keyLengthInBytes = keyLengthInBytes;
        this.keyBuffer = ByteBuffer.allocate(keyLengthInBytes);
        this.keyEncoder = keyEncoder;
        this.keySpace = ByteBuffer.allocate(keyLengthInBytes * capacity);
        this.values = new Object[capacity];
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean containsKey(final Object key)
    {
        final K typedKey = (K) key;
        final int keySpaceIndex = getKeySpaceIndex(typedKey);

        return keyIsAtIndex(typedKey, keySpaceIndex);
    }

    private boolean keyIsAtIndex(final K typedKey, final int keySpaceIndex)
    {
        keyBuffer.clear();
        keyEncoder.accept(typedKey, keyBuffer);
        final int startPosition = keySpaceIndex * keyLengthInBytes;
        for(int i = 0; i < keyLengthInBytes; i++)
        {
            if(keySpace.get(startPosition + i) != keyBuffer.get(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsValue(final Object value)
    {
        return false;
    }

    @Override
    public V get(final Object key)
    {
        final K typedKey = (K) key;
        final int keySpaceIndex = getKeySpaceIndex(typedKey);

        if(keyIsAtIndex(typedKey, keySpaceIndex))
        {
            return (V) values[keySpaceIndex];
        }

        return null;
    }

    @Override
    public V put(final K key, final V value)
    {
        final int keySpaceIndex = getKeySpaceIndex(key);
        keySpace.position(keySpaceIndex * keyLengthInBytes);
        keySpace.put(keyBuffer);
        final V previous = (V) values[keySpaceIndex];
        values[keySpaceIndex] = value;

        return previous;
    }

    private int getKeySpaceIndex(final K key)
    {
        keyBuffer.clear();
        keyEncoder.accept(key, keyBuffer);

        if(keyBuffer.position() != keyLengthInBytes)
        {
            throw new IllegalStateException("Key encoder did not produce expected number of bytes");
        }

        keyBuffer.flip();
        int hashCode = 0;
        while(keyBuffer.remaining() > 3)
        {
            int keyPart = keyBuffer.getInt();
            hashCode ^= keyPart ^ (keyPart >>> 16);
        }
        keyBuffer.rewind();

        return (hashCode & capacity - 1);
    }

    @Override
    public V remove(final Object key)
    {
        return null;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m)
    {

    }

    @Override
    public void clear()
    {

    }

    @Override
    public Set<K> keySet()
    {
        return null;
    }

    @Override
    public Collection<V> values()
    {
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return null;
    }

}