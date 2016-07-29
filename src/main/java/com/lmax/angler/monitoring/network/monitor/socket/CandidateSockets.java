package com.lmax.angler.monitoring.network.monitor.socket;

import org.agrona.collections.Long2ObjectHashMap;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe repository for sockets requested for monitoring.
 */
public final class CandidateSockets
{
    private static final float AGRONA_DEFAULT_LOAD_FACTOR = 0.67f;

    private final AtomicReference<Long2ObjectHashMap<InetSocketAddress>> candidateSockets =
            new AtomicReference<>(new Long2ObjectHashMap<>());

    public void beginMonitoringSocketIdentifier(final InetSocketAddress socketAddress, final long socketIdentifier)
    {
        while (true)
        {
            final Long2ObjectHashMap<InetSocketAddress> candidateSnapshot = candidateSockets.get();
            final Long2ObjectHashMap<InetSocketAddress> updated = new Long2ObjectHashMap<>(candidateSnapshot.size(), AGRONA_DEFAULT_LOAD_FACTOR);

            final Long2ObjectHashMap<InetSocketAddress>.KeyIterator keyIterator = candidateSnapshot.keySet().iterator();
            while(keyIterator.hasNext())
            {
                final long key = keyIterator.nextLong();
                updated.put(key, candidateSnapshot.get(key));
            }

            updated.put(socketIdentifier, socketAddress);

            if (candidateSockets.compareAndSet(candidateSnapshot, updated))
            {
                break;
            }
        }
    }

    public void endMonitoringOfSocketIdentifier(final long socketIdentifier)
    {
        while (true)
        {
            final Long2ObjectHashMap<InetSocketAddress> candidateSnapshot = candidateSockets.get();
            final Long2ObjectHashMap<InetSocketAddress> updated = new Long2ObjectHashMap<>(candidateSnapshot.size(), AGRONA_DEFAULT_LOAD_FACTOR);

            final Long2ObjectHashMap<InetSocketAddress>.KeyIterator keyIterator = candidateSnapshot.keySet().iterator();
            while(keyIterator.hasNext())
            {
                final long key = keyIterator.nextLong();
                if(key != socketIdentifier)
                {
                    updated.put(key, candidateSnapshot.get(key));
                }
            }

            if (candidateSockets.compareAndSet(candidateSnapshot, updated))
            {
                break;
            }
        }
    }

    public Long2ObjectHashMap<InetSocketAddress> getSnapshot()
    {
        return candidateSockets.get();
    }
}
