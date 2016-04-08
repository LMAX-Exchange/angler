package com.epickrram.monitoring.network;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import static com.epickrram.monitoring.network.SendingRates.constant;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

public final class ReceiverOverrunMain
{
    private static final int PUBLISH_INTERVAL = 0;
    private static final long RECEIVE_INTERVAL = 10;
    private static final double RECEIVER_SLOWDOWN_FACTOR = 10.2d;

    public static void main(final String[] args)
    {
        final LongUnaryOperator sendingDelayCalculator = constant(PUBLISH_INTERVAL, MICROSECONDS);
        final LongConsumer transmitLatencyHandler = l -> {
            final long receiverDelayPerMessage = (long)
                    (TimeUnit.MILLISECONDS.toNanos(RECEIVE_INTERVAL) *
                            RECEIVER_SLOWDOWN_FACTOR);
            LockSupport.parkNanos(receiverDelayPerMessage);
        };

        final ExperimentConfig config = args.length == 1 ?
                ExperimentConfig.withAddress(args[0]) : ExperimentConfig.defaults();
        new Experiment(sendingDelayCalculator, transmitLatencyHandler, config).execute();
    }
}