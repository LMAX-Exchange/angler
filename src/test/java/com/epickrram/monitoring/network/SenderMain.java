package com.epickrram.monitoring.network;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongUnaryOperator;

import static com.epickrram.monitoring.network.SendingRates.constant;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class SenderMain
{
    public static void main(final String[] args)
    {
        final ExperimentConfig config = args.length == 1 ?
                ExperimentConfig.withAddress(args[0]) : ExperimentConfig.defaults();

        final LongUnaryOperator sendingDelayCalculator = constant(10, MILLISECONDS);
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
        final MessageFactory messageFactory = new MessageFactory();
        final UnicastSender sender = new UnicastSender(config.getAddress(), sendingDelayCalculator, messageFactory::prepare);
        final SendReceiveCountMonitor sendReceiveCountMonitor =
                new SendReceiveCountMonitor(sender::getSentCount, () -> -1);
        executorService.submit(sender::sendLoop);

        executorService.scheduleAtFixedRate(() -> {
            sendReceiveCountMonitor.report();
            System.out.println();
        }, 1L, 1L, SECONDS);


        LockSupport.parkNanos(config.getExperimentRuntimeUnit().toNanos(config.getExperimentRuntimeDuration()));

        executorService.shutdownNow();
        try
        {
            if(!executorService.awaitTermination(10, TimeUnit.SECONDS))
            {
                System.err.println("Failed to shutdown executor, threads did not exit.");
            }
        }
        catch (InterruptedException e)
        {
            System.err.println("Interrupted while waiting for threads to exit.");
        }
    }
}
