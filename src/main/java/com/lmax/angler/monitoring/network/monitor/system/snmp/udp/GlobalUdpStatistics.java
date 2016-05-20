package com.lmax.angler.monitoring.network.monitor.system.snmp.udp;

public final class GlobalUdpStatistics
{
    private long inErrors;
    private long receiveBufferErrors;
    private long checksumErrors;

    public long getInErrors()
    {
        return inErrors;
    }

    void setInErrors(final long inErrors)
    {
        this.inErrors = inErrors;
    }

    public long getReceiveBufferErrors()
    {
        return receiveBufferErrors;
    }

    void setReceiveBufferErrors(final long receiveBufferErrors)
    {
        this.receiveBufferErrors = receiveBufferErrors;
    }

    public long getChecksumErrors()
    {
        return checksumErrors;
    }

    void setChecksumErrors(final long checksumErrors)
    {
        this.checksumErrors = checksumErrors;
    }

    void reset()
    {
        inErrors = 0;
        receiveBufferErrors = 0;
        checksumErrors = 0;
    }
}
