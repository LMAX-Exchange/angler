package com.epickrram.monitoring.network;

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

        new Experiment(sendingDelayCalculator, transmitLatencyHandler, ExperimentConfig.defaults()).execute();
    }
}