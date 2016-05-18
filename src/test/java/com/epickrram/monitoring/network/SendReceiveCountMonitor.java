package com.epickrram.monitoring.network;

import java.util.function.LongSupplier;

public final class SendReceiveCountMonitor
{
    private final LongSupplier sentCount;
    private final LongSupplier receivedCount;

    public SendReceiveCountMonitor(LongSupplier sentCount, LongSupplier receivedCount)
    {
        this.sentCount = sentCount;
        this.receivedCount = receivedCount;
    }

    public void report()
    {
        System.out.printf("Sent: %d, Received: %d%n", sentCount.getAsLong(), receivedCount.getAsLong());
    }
}
