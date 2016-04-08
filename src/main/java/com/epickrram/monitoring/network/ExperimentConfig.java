package com.epickrram.monitoring.network;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
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

    public static ExperimentConfig defaults()
    {
        final TimeUnit experimentRuntimeUnit = TimeUnit.SECONDS;
        final long experimentRuntimeDuration = 30L;
        final InetSocketAddress address;
        try
        {
            address = new InetSocketAddress(Inet4Address.getLocalHost(), 51000);
        }
        catch (final UnknownHostException e)
        {
            throw new IllegalStateException("Could not get localhost", e);
        }

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
