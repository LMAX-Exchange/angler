package com.epickrram.monitoring.network.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

public final class NetstatUdpStatsMonitor
{
    private final UdpStats udpStats = new UdpStats();

    public void report()
    {
        final Process process;
        try
        {
            process = new ProcessBuilder("netstat", "-su").start();
            if(0 != process.waitFor())
            {
                System.err.println("Failed to execute netstat command.");
                return;
            }

            try(final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                udpStats.reset();
                reader.lines().forEach(l -> {
                    if(l.contains("receive buffer errors")) //or: RcvbufErrors: {value}
                    {
                        udpStats.updateBufferErrors(Long.parseLong(l.trim().split("\\s+")[0]));
                    }
                    else if(l.contains("RcvbufErrors"))
                    {
                        udpStats.updateBufferErrors(Long.parseLong(l.trim().split("\\s+")[1]));
                    }
                    else if(l.contains("packet receive errors"))
                    {
                        udpStats.updateReceiveErrors(Long.parseLong(l.trim().split("\\s+")[0]));
                    }
                });

                if(udpStats.changed)
                {
                    System.out.printf("Netstat buffer errors: %d, receive errors: %d%n",
                            udpStats.receiveBufferErrors, udpStats.packetReceiveErrors);
                }
            }
        }
        catch (final IOException e)
        {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
        catch (final InterruptedException e)
        {
            System.err.println("Interrupted while waiting for process.");
        }
    }

    private static final class UdpStats
    {
        private static final int UNSET = -1;

        private long packetReceiveErrors = UNSET;
        private long receiveBufferErrors = UNSET;
        private boolean changed;

        void updateReceiveErrors(final long packetReceiveErrors)
        {
            if(packetReceiveErrors != this.packetReceiveErrors && this.packetReceiveErrors != UNSET)
            {
                changed = true;
            }
            this.packetReceiveErrors = packetReceiveErrors;
        }

        void updateBufferErrors(final long receiveBufferErrors)
        {
            if(receiveBufferErrors != this.receiveBufferErrors && this.receiveBufferErrors != UNSET)
            {
                changed = true;
            }
            this.receiveBufferErrors = receiveBufferErrors;
        }

        void reset()
        {
            changed = false;
        }
    }
}
