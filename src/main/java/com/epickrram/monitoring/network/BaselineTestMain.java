package com.epickrram.monitoring.network;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import static com.epickrram.monitoring.network.SendingRates.constant;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class BaselineTestMain
{
    public static void main(final String[] args) throws Exception
    {
        final LongUnaryOperator sendingDelayCalculator = constant(100, MILLISECONDS);
        final LongConsumer transmitLatencyHandler = l -> {};
        final TimeUnit experimentRuntimeUnit = TimeUnit.SECONDS;
        final long experimentRuntimeDuration = 30L;
        final InetSocketAddress address = new InetSocketAddress(Inet4Address.getLocalHost(), 51000);

        new Experiment(sendingDelayCalculator, transmitLatencyHandler, address, experimentRuntimeUnit, experimentRuntimeDuration).execute();
    }
}