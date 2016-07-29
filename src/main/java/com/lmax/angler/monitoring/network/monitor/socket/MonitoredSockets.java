package com.lmax.angler.monitoring.network.monitor.socket;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongHashSet;
import org.agrona.collections.LongIterator;

/**
 * Repository for sockets that are currently being monitored.
 * @param <T> a socket
 */
public final class MonitoredSockets<T extends DescribableSocket>
{
    private final Long2ObjectHashMap<T> monitoredSocketInstances = new Long2ObjectHashMap<>();
    private final SocketMonitoringLifecycleListener lifecycleListener;
    private final SocketDescriptor socketDescriptor = new SocketDescriptor();
    private final LongHashSet keysForRemoval = new LongHashSet(Long.MIN_VALUE);

    public MonitoredSockets(final SocketMonitoringLifecycleListener lifecycleListener)
    {
        this.lifecycleListener = lifecycleListener;
    }

    public boolean contains(final long socketInstanceIndentifier)
    {
        return monitoredSocketInstances.containsKey(socketInstanceIndentifier);
    }

    public void put(final long socketInstanceIndentifier, final T entry)
    {
        monitoredSocketInstances.put(socketInstanceIndentifier, entry);
        entry.describeTo(socketDescriptor);

        lifecycleListener.socketMonitoringStarted(
                socketDescriptor.getAddress(), socketDescriptor.getPort(), socketDescriptor.getInode());
    }

    public T get(final long socketInstanceIndentifier)
    {
        return monitoredSocketInstances.get(socketInstanceIndentifier);
    }

    public void purgeEntriesOlderThan(final long latestUpdateCount)
    {
        keysForRemoval.clear();
        final Long2ObjectHashMap<T>.KeyIterator iterator = monitoredSocketInstances.keySet().iterator();
        while(iterator.hasNext())
        {
            final long key = iterator.nextLong();
            if(monitoredSocketInstances.get(key).getUpdateCount() != latestUpdateCount)
            {
                keysForRemoval.add(key);
            }
        }

        final LongIterator keyIterator = keysForRemoval.iterator();
        while(keyIterator.hasNext())
        {
            final long key = keyIterator.nextValue();
            final T staleEntry = monitoredSocketInstances.remove(key);
            staleEntry.describeTo(socketDescriptor);
            lifecycleListener.socketMonitoringStopped(
                    socketDescriptor.getAddress(), socketDescriptor.getPort(), socketDescriptor.getInode());
        }
    }
}