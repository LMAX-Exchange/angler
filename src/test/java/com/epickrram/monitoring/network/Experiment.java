package com.epickrram.monitoring.network;

import com.epickrram.monitoring.network.monitor.KernelBufferDepthMonitor;
import com.epickrram.monitoring.network.monitor.system.NetstatUdpStatsMonitor;
import com.epickrram.monitoring.network.monitor.system.SoftIrqHandlerTimeSqueezeMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class Experiment
{
    private final LongUnaryOperator sendingDelayCalculator;
    private final LongConsumer transmitLatencyHandler;
    private final TimeUnit experimentRuntimeUnit;
    private final long experimentRuntimeDuration;
    private final SocketAddress address;
    private final boolean shouldRunSender;

    public Experiment(
            final LongUnaryOperator sendingDelayCalculator,
            final LongConsumer transmitLatencyHandler,
            final ExperimentConfig config)
    {
        this.sendingDelayCalculator = sendingDelayCalculator;
        this.transmitLatencyHandler = transmitLatencyHandler;
        this.address = config.getAddress();
        this.experimentRuntimeUnit = config.getExperimentRuntimeUnit();
        this.experimentRuntimeDuration = config.getExperimentRuntimeDuration();
        this.shouldRunSender = config.shouldRunSender();
    }

    void execute()
    {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
        final MessageFactory messageFactory = new MessageFactory();
        final UnicastReceiver receiver = new UnicastReceiver(address, transmitLatencyHandler);
        final UnicastSender sender = new UnicastSender(address, sendingDelayCalculator, messageFactory::prepare);
        final KernelBufferDepthMonitor bufferDepthMonitor = new KernelBufferDepthMonitor(address);
        final SoftIrqHandlerTimeSqueezeMonitor timeSqueezeMonitor = new SoftIrqHandlerTimeSqueezeMonitor();
        final NetstatUdpStatsMonitor netstatUdpStatsMonitor = new NetstatUdpStatsMonitor();
        final SendReceiveCountMonitor sendReceiveCountMonitor =
                new SendReceiveCountMonitor(shouldRunSender ? sender::getSentCount : () -> -1, receiver::getReceivedCount);

        executorService.submit(logExit("receiver", receiver::receiveLoop));

        System.out.printf("Receiver SO_RCVBUF set to %d%n%n", receiver.getConfiguredReceiveBufferSize());

        if(shouldRunSender)
        {
            executorService.submit(logExit("sender", sender::sendLoop));
        }

        executorService.scheduleAtFixedRate(logExit("monitor", () -> {
            try
            {
                sendReceiveCountMonitor.report();
                bufferDepthMonitor.report();
                timeSqueezeMonitor.report();
                netstatUdpStatsMonitor.report();
            }
            catch(final RuntimeException e)
            {
                e.printStackTrace();
            }
            System.out.println();

        }), 1L, 1L, SECONDS);


        LockSupport.parkNanos(experimentRuntimeUnit.toNanos(experimentRuntimeDuration));

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

    private static Runnable logExit(final String taskName, final Runnable delegate)
    {
        return new ExitLoggingRunnable(delegate, taskName);
    }

    private static final class ExitLoggingRunnable implements Runnable
    {
        private static final Logger LOGGER = LoggerFactory.getLogger(ExitLoggingRunnable.class);

        private final Runnable delegate;
        private final String name;

        public ExitLoggingRunnable(final Runnable delegate, final String name)
        {
            this.delegate = delegate;
            this.name = name;
        }

        @Override
        public void run()
        {
            try
            {
                delegate.run();
            }
            catch(final Throwable t)
            {
                LOGGER.error("Task {} exited with exception: {}", name, t.getMessage());
            }
            finally
            {
                LOGGER.info("Task {} exited normally", name);
            }
        }
    }
}
