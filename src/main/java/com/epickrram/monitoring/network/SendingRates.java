package com.epickrram.monitoring.network;

import java.util.concurrent.TimeUnit;
import java.util.function.LongUnaryOperator;

public final class SendingRates
{
    private SendingRates() {}

    public static LongUnaryOperator constant(final long interval, final TimeUnit timeUnit)
    {
        return d -> timeUnit.toNanos(interval);
    }
}
