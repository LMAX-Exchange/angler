package com.epickrram.monitoring.network;

import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import static com.epickrram.monitoring.network.SendingRates.constant;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

public final class BaselineTestMain
{
    public static void main(final String[] args) throws Exception
    {
        final LongUnaryOperator sendingDelayCalculator = constant(10, MICROSECONDS);
        final LongConsumer transmitLatencyHandler = l -> {};

        final ExperimentConfig config = args.length == 1 ?
                ExperimentConfig.withAddress(args[0]) : ExperimentConfig.defaults();
        new Experiment(sendingDelayCalculator, transmitLatencyHandler, config).execute();
    }
}