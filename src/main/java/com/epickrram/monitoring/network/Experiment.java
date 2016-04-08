package com.epickrram.monitoring.network;

import com.epickrram.monitoring.network.monitor.KernelBufferDepthMonitor;
import com.epickrram.monitoring.network.monitor.NetstatUdpStatsMonitor;
import com.epickrram.monitoring.network.monitor.SendReceiveCountMonitor;
import com.epickrram.monitoring.network.monitor.SoftIrqHandlerTimeSqueezeMonitor;

import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import static com.epickrram.monitoring.network.AffinityWrapper.runOnThread;
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

        executorService.submit(runOnThread(29, receiver::receiveLoop));

        System.out.printf("Receiver SO_RCVBUF set to %d%n%n", receiver.getConfiguredReceiveBufferSize());

        if(shouldRunSender)
        {
            executorService.submit(runOnThread(31, sender::sendLoop));
        }

        executorService.scheduleAtFixedRate(() -> {
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

        }, 1L, 1L, SECONDS);


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
}
