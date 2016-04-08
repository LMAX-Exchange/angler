package com.epickrram.monitoring.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public final class ExperimentConfig
{
    private final TimeUnit experimentRuntimeUnit;
    private final long experimentRuntimeDuration;
    private final SocketAddress address;

    public ExperimentConfig(
            final TimeUnit experimentRuntimeUnit,
            final long experimentRuntimeDuration,
            final SocketAddress address)
    {
        this.experimentRuntimeUnit = experimentRuntimeUnit;
        this.experimentRuntimeDuration = experimentRuntimeDuration;
        this.address = address;
    }

    public static ExperimentConfig withAddress(final String addressSpec)
    {
        final String[] hostPort = addressSpec.split(":");
        return withAddress(new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1])));
    }

    public static ExperimentConfig defaults()
    {
        final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 51000);
        return withAddress(address);
    }

    private static ExperimentConfig withAddress(final InetSocketAddress address)
    {
        final TimeUnit experimentRuntimeUnit = TimeUnit.SECONDS;
        final long experimentRuntimeDuration = 30L;

        return new ExperimentConfig(experimentRuntimeUnit, experimentRuntimeDuration, address);
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
}
