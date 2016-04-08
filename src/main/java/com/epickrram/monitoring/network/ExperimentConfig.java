package com.epickrram.monitoring.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public final class ExperimentConfig
{
    static final InetSocketAddress DEFAULT_ADDRESS =
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 51000);
    private final TimeUnit experimentRuntimeUnit;
    private final long experimentRuntimeDuration;
    private final SocketAddress address;
    private final boolean runSender;

    public ExperimentConfig(
            final TimeUnit experimentRuntimeUnit,
            final long experimentRuntimeDuration,
            final SocketAddress address,
            final boolean runSender)
    {
        this.experimentRuntimeUnit = experimentRuntimeUnit;
        this.experimentRuntimeDuration = experimentRuntimeDuration;
        this.address = address;
        this.runSender = runSender;
    }

    public static ExperimentConfig withAddress(final String addressSpec)
    {
        final InetSocketAddress address = parseAddress(addressSpec);
        return withAddress(address);
    }

    public static ExperimentConfig defaults()
    {
        return withAddress(DEFAULT_ADDRESS);
    }

    static InetSocketAddress parseAddress(final String addressSpec)
    {
        final String[] hostPort = addressSpec.split(":");
        return new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1]));
    }

    private static ExperimentConfig withAddress(final InetSocketAddress address)
    {
        final TimeUnit experimentRuntimeUnit = TimeUnit.SECONDS;
        final long experimentRuntimeDuration = 30L;

        return new ExperimentConfig(experimentRuntimeUnit, experimentRuntimeDuration, address, true);
    }

    public TimeUnit getExperimentRuntimeUnit()
    {
        return experimentRuntimeUnit;
    }

    public long getExperimentRuntimeDuration()
    {
        return experimentRuntimeDuration;
    }

    public SocketAddress getAddress()
    {
        return address;
    }

    public boolean shouldRunSender()
    {
        return runSender;
    }
}
