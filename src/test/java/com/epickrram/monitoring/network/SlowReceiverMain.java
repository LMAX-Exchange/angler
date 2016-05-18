package com.epickrram.monitoring.network;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import static com.epickrram.monitoring.network.SendingRates.constant;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class SlowReceiverMain
{
    private static final int PUBLISH_INTERVAL = 100;
    private static final double RECEIVER_SLOWDOWN_FACTOR = 1.2d;

    public static void main(final String[] args)
    {
        final LongUnaryOperator sendingDelayCalculator = constant(PUBLISH_INTERVAL, MILLISECONDS);
        final LongConsumer transmitLatencyHandler = l -> {
            final long receiverDelayPerMessage = (long)
                    (TimeUnit.MILLISECONDS.toNanos(PUBLISH_INTERVAL) *
                            RECEIVER_SLOWDOWN_FACTOR);
            LockSupport.parkNanos(receiverDelayPerMessage);
        };

        final SocketAddress address = args.length == 1 ?
                ExperimentConfig.parseAddress(args[0]) :
                ExperimentConfig.DEFAULT_ADDRESS;

        final ExperimentConfig config = new ExperimentConfig(SECONDS, 30L, address, false);
        new Experiment(sendingDelayCalculator, transmitLatencyHandler, config).execute();
    }
}