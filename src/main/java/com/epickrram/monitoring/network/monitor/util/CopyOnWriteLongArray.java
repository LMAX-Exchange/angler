package com.epickrram.monitoring.network.monitor.util;

import java.util.concurrent.atomic.AtomicReference;

public final class CopyOnWriteLongArray
{
    private final AtomicReference<long[]> data = new AtomicReference<>(new long[64]);
}
