package com.lmax.angler.monitoring.network.monitor.util;

import org.agrona.BitUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/**
 * Primarily copied from Agrona Long2ObjectHashMap, with scope for arbitrarily long keys.
 * https://github.com/real-logic/Agrona/
 *
 * Keys must be fixed-length, and capable of being encoded into a ByteBuffer.
 *
 * @param <V> the value type
 */
public final class EncodedData2ObjectHashMap<K, V> implements Map<K, V>
{
    private static final int KEY_NOT_FOUND = -1;

    private final int keyLengthInBytes;
    private final ByteBuffer keyBuffer;
    private final ByteBuffer nullKeyBuffer;
    private final float loadFactor;
    private final BiConsumer<K, ByteBuffer> keyEncoder;
    private final ToIntFunction<ByteBuffer> hashFunction;

    private ByteBuffer keySpace;
    private Object[] values;
    private int resizeThreshold;
    private int capacity;
    private int size;

    public EncodedData2ObjectHashMap(
            final int initialCapacity,
            final float loadFactor,
            final int keyLengthInBytes,
            final BiConsumer<K, ByteBuffer> keyEncoder,
            final ToIntFunction<ByteBuffer> hashFunction,
            final K nullKey)
    {
        this.capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        this.loadFactor = loadFactor;
        this.keyLengthInBytes = keyLengthInBytes;
        this.keyBuffer = ByteBuffer.allocate(keyLengthInBytes);
        this.nullKeyBuffer = ByteBuffer.allocate(keyLengthInBytes);
        this.keyEncoder = keyEncoder;
        this.keySpace = ByteBuffer.allocate(keyLengthInBytes * capacity);
        this.values = new Object[capacity];
        this.hashFunction = hashFunction;

        keyEncoder.accept(nullKey, nullKeyBuffer);
        fillKeySpaceWithNullKey(keySpace, capacity);
    }

    public EncodedData2ObjectHashMap(
            final int initialCapacity,
            final float loadFactor,
            final int keyLengthInBytes,
            final BiConsumer<K, ByteBuffer> keyEncoder,
            final K nullKey)
    {
        this(initialCapacity, loadFactor, keyLengthInBytes, keyEncoder, EncodedData2ObjectHashMap::defaultHash, nullKey);
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public boolean isEmpty()
    {
        return size == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(final Object key)
    {
        final K typedKey = (K) key;
        keyBuffer.clear();
        keyEncoder.accept(typedKey, keyBuffer);
        final int initialKeySpaceIndex = getInitialKeySpaceIndex(typedKey);
        final int actualKeySpaceIndex = findKeySpaceIndex(initialKeySpaceIndex, keyBuffer);
        return actualKeySpaceIndex != KEY_NOT_FOUND;
    }

    @Override
    public boolean containsValue(final Object value)
    {
        for (int i = 0; i < values.length; i++)
        {
            if(value ==  values[i])
            {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(final Object key)
    {
        final K typedKey = (K) key;
        keyBuffer.clear();
        keyEncoder.accept(typedKey, keyBuffer);
        final int initialKeySpaceIndex = getInitialKeySpaceIndex(typedKey);
        final int actualKeySpaceIndex = findKeySpaceIndex(initialKeySpaceIndex, keyBuffer);
        if(actualKeySpaceIndex != KEY_NOT_FOUND)
        {
            return (V) values[actualKeySpaceIndex];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(final K key, final V value)
    {
        increaseSizeIfRequired();

        final int initialKeySpaceIndex = getInitialKeySpaceIndex(key);
        final int existingOrEmptyKeySpaceIndex = findWritableIndexForKey(initialKeySpaceIndex, keySpace, keyBuffer);

        this.keySpace.position(existingOrEmptyKeySpaceIndex * keyLengthInBytes);
        this.keySpace.put(keyBuffer);
        final V previous = (V) values[existingOrEmptyKeySpaceIndex];
        values[existingOrEmptyKeySpaceIndex] = value;
        size++;
        return previous;
    }

    private int findWritableIndexForKey(final int initialKeySpaceIndex, final ByteBuffer searchSpace, final ByteBuffer keyBuffer)
    {
        int currentKeySpaceIndex = initialKeySpaceIndex;
        while((!encodedKeyIsAtIndex(currentKeySpaceIndex, nullKeyBuffer, searchSpace)) &&
                (!encodedKeyIsAtIndex(currentKeySpaceIndex, keyBuffer, searchSpace)))
        {
            currentKeySpaceIndex++;
            currentKeySpaceIndex = currentKeySpaceIndex & searchSpace.capacity() - 1;

            if((currentKeySpaceIndex) == initialKeySpaceIndex)
            {
                throw new IllegalStateException("Could not find existing or empty slot for key");
            }
        }
        return currentKeySpaceIndex;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(final Object key)
    {
        final K typedKey = (K) key;
        keyBuffer.clear();
        keyEncoder.accept(typedKey, keyBuffer);
        final int initialKeySpaceIndex = getInitialKeySpaceIndex(typedKey);
        final int actualKeySpaceIndex = findKeySpaceIndex(initialKeySpaceIndex, keyBuffer);
        if(actualKeySpaceIndex != KEY_NOT_FOUND)
        {
            final V existingValue = (V) values[actualKeySpaceIndex];
            values[actualKeySpaceIndex] = null;
            removeKeyAtIndex(actualKeySpaceIndex);
            size--;
            return existingValue;
        }

        return null;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        Arrays.fill(values, null);
        for(int i = 0; i < keySpace.capacity() / keyLengthInBytes; i++)
        {
            keySpace.position(i * keyLengthInBytes);
            nullKeyBuffer.clear();
            keySpace.put(nullKeyBuffer);
        }
        keySpace.clear();
        size = 0;
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

    private boolean encodedKeyIsAtIndex(final int keySpaceIndex, final ByteBuffer encodedKey, final ByteBuffer searchSpace)
    {
        final int startPosition = keySpaceIndex * keyLengthInBytes;
        for(int i = 0; i < keyLengthInBytes; i++)
        {
            if(searchSpace.get(startPosition + i) != encodedKey.get(i))
            {
                encodedKey.rewind();
                return false;
            }
        }
        encodedKey.rewind();
        return true;
    }

    private void removeKeyAtIndex(final int keySpaceIndex)
    {
        nullKeyBuffer.clear();
        keySpace.position(keySpaceIndex * keyLengthInBytes);
        // TODO need nullKey + hash check, or separate empty index indicator
        keySpace.put(nullKeyBuffer);
        // TODO needs to compact any null keys
    }

    private int findKeySpaceIndex(final int initialKeySpaceIndex, final ByteBuffer encodedKey)
    {
        int currentKeySpaceIndex = initialKeySpaceIndex;
        while((!encodedKeyIsAtIndex(currentKeySpaceIndex, encodedKey, keySpace)))
        {
            currentKeySpaceIndex++;
            currentKeySpaceIndex = currentKeySpaceIndex & capacity - 1;

            if((currentKeySpaceIndex) == initialKeySpaceIndex)
            {
                return KEY_NOT_FOUND;
            }
        }

        return currentKeySpaceIndex;
    }

    private int getInitialKeySpaceIndex(final K key)
    {
        keyBuffer.clear();
        keyEncoder.accept(key, keyBuffer);

        if(keyBuffer.position() != keyLengthInBytes)
        {
            throw new IllegalStateException("Key encoder did not produce expected number of bytes");
        }

        keyBuffer.flip();
        final int hashCode = hashFunction.applyAsInt(keyBuffer);
        keyBuffer.rewind();

        return (hashCode & capacity - 1);
    }

    private void fillKeySpaceWithNullKey(final ByteBuffer keySpace, final int numberOfKeys)
    {
        for(int i = 0; i < numberOfKeys; i++)
        {
            nullKeyBuffer.rewind();
            keySpace.position(i * keyLengthInBytes);
            keySpace.put(nullKeyBuffer);
        }
        keySpace.clear();
    }

    private void increaseSizeIfRequired()
    {
        if(size >= loadFactor * capacity)
        {
            final int newKeySpaceCapacity = keySpace.capacity() * 2;
            final int newMapCapacity = capacity * 2;
            final ByteBuffer increasedKeySpace = ByteBuffer.allocate(newKeySpaceCapacity);
            fillKeySpaceWithNullKey(increasedKeySpace, newMapCapacity);
            final Object[] newValues = new Object[newMapCapacity];

            for(int i = 0; i < capacity; i++)
            {
                keySpace.position(i * keyLengthInBytes);
                keySpace.limit(keySpace.position() + keyLengthInBytes);
                keyBuffer.clear();
                keyBuffer.put(keySpace);

                keyBuffer.flip();
                if(buffersAreEqual(keyBuffer, nullKeyBuffer))
                {
                    continue;
                }
                final int hashCode = hashFunction.applyAsInt(keyBuffer);
                keyBuffer.rewind();
                final int initialKeySpaceIndex = (hashCode & newMapCapacity - 1);
                final int existingOrEmptyKeySpaceIndex = findWritableIndexForKey(initialKeySpaceIndex, increasedKeySpace, keyBuffer);

                increasedKeySpace.position(existingOrEmptyKeySpaceIndex * keyLengthInBytes);
                increasedKeySpace.put(keyBuffer);

                newValues[existingOrEmptyKeySpaceIndex] = values[i];
            }

            capacity = newMapCapacity;
            keySpace = increasedKeySpace;
            values = newValues;
        }
    }

    private static boolean buffersAreEqual(final ByteBuffer a, final ByteBuffer b)
    {
        if(a.capacity() != b.capacity())
        {
            return false;
        }
        for(int i = 0; i < a.capacity(); i++)
        {
            if(a.get(i) != b.get(i))
            {
                return false;
            }
        }

        return true;
    }

    private static int defaultHash(final ByteBuffer keyBuffer)
    {
        int hashCode = 0;
        while(keyBuffer.remaining() > 3)
        {
            int keyPart = keyBuffer.getInt();
            hashCode ^= keyPart ^ (keyPart >>> 16);
        }
        return hashCode;
    }
}